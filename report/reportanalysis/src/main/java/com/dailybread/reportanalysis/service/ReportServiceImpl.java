package com.dailybread.reportanalysis.service;

import com.dailybread.reportanalysis.dto.ReportDTO;
import com.dailybread.reportanalysis.entity.ReportEntry;
import com.dailybread.reportanalysis.repository.ReportRepository;
import com.dailybread.reportanalysis.exception.ResourceNotFoundException;
import com.dailybread.reportanalysis.feign.InventoryClient;
import com.dailybread.reportanalysis.feign.PosClient;
import com.dailybread.reportanalysis.dto.Inventory;
import com.dailybread.reportanalysis.dto.GetSaleResponse;
import com.dailybread.reportanalysis.dto.SaleItem;
import com.dailybread.reportanalysis.dto.WeeklySalesData;
import com.dailybread.reportanalysis.dto.MonthlySalesData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime; // Import ZonedDateTime
import java.time.temporal.WeekFields;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final PosClient posClient;
    private final InventoryClient inventoryClient;

    private List<WeeklySalesData> cachedWeeklySales;
    private List<MonthlySalesData> cachedMonthlySales;

    @Override
    public List<ReportDTO> getAllReports() {
        return reportRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ReportDTO getReportByItem(String itemName) {
        ReportEntry entry = reportRepository.findAll().stream()
                .filter(r -> r.getItemName().equalsIgnoreCase(itemName))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item not found"));
        return convertToDTO(entry);
    }

    @Override
    @Transactional
    public void fetchAndGenerateReports(Integer offsetHours) {
        System.out.println("--- Starting report generation ---");

        // Determine the ZoneOffset based on the provided offsetHours
        // If offsetHours is null (e.g., direct call without param or testing), default
        // to UTC (0)
        ZoneOffset clientZoneOffset = ZoneOffset.ofHours(offsetHours != null ? offsetHours : 0);
        System.out.println("REPORT_SERVICE: Client Zone Offset calculated: " + clientZoneOffset);

        List<GetSaleResponse> tempSalesList;
        try {
            tempSalesList = posClient.getAllSaleResponses();
            System.out.println("REPORT_SERVICE: Fetched " + (tempSalesList != null ? tempSalesList.size() : 0)
                    + " sales from POS.");
        } catch (Exception e) {
            System.err.println("REPORT_SERVICE: Error fetching sales from POS: " + e.getMessage());
            e.printStackTrace();
            tempSalesList = new ArrayList<>();
        }
        final List<GetSaleResponse> salesList = tempSalesList;

        List<Inventory> tempInventoryList;
        try {
            tempInventoryList = inventoryClient.getAllInventory();
            System.out.println("REPORT_SERVICE: Fetched " + (tempInventoryList != null ? tempInventoryList.size() : 0)
                    + " inventory items from Inventory.");
        } catch (Exception e) {
            System.err.println("REPORT_SERVICE: Error fetching inventory from Inventory: " + e.getMessage());
            e.printStackTrace();
            tempInventoryList = new ArrayList<>();
        }
        final List<Inventory> inventoryList = tempInventoryList;

        if (salesList.isEmpty() && inventoryList.isEmpty()) {
            System.out.println("REPORT_SERVICE: No sales or inventory data available to generate reports.");
            reportRepository.deleteAll();
            cachedWeeklySales = new ArrayList<>();
            cachedMonthlySales = new ArrayList<>();
            return;
        }

        Map<Long, Inventory> inventoryMap = inventoryList.stream()
                .collect(Collectors.toMap(Inventory::getId, inventory -> inventory));

        reportRepository.deleteAll();
        System.out.println("REPORT_SERVICE: Cleared existing reports from database.");

        Map<String, Double> itemRevenue = salesList.stream()
                .flatMap(sales -> sales.getItems().stream())
                .collect(Collectors.groupingBy(item -> {
                    Inventory inv = inventoryMap.get(item.getItemId());
                    return inv != null ? inv.getName() : "Unknown Product (ID: " + item.getItemId() + ")";
                }, Collectors.summingDouble(item -> item.getQuantity() * item.getPrice())));

        Map<String, Integer> itemQuantitySold = salesList.stream()
                .flatMap(sales -> sales.getItems().stream())
                .collect(Collectors.groupingBy(item -> {
                    Inventory inv = inventoryMap.get(item.getItemId());
                    return inv != null ? inv.getName() : "Unknown Product (ID: " + item.getItemId() + ")";
                }, Collectors.summingInt(SaleItem::getQuantity)));

        itemRevenue.forEach((itemName, totalRevenue) -> {
            Integer quantitySold = itemQuantitySold.getOrDefault(itemName, 0);
            Inventory associatedInventory = inventoryList.stream()
                    .filter(inv -> inv.getName().equals(itemName))
                    .findFirst()
                    .orElse(null);

            ReportEntry reportEntry = ReportEntry.builder()
                    .itemName(itemName)
                    .quantitySold(quantitySold)
                    .totalRevenue(totalRevenue)
                    .remainingStock(associatedInventory != null ? associatedInventory.getStock() : 0)
                    .category(associatedInventory != null ? associatedInventory.getCategory() : "Unknown")
                    .build();
            reportRepository.save(reportEntry);
            System.out.println("REPORT_SERVICE: Saved ReportEntry for: " + itemName + " (Qty: " + quantitySold
                    + ", Revenue: " + totalRevenue + ")");
        });

        // --- Weekly Sales Aggregation ---
        Map<DayOfWeek, Double> weeklySalesMap = salesList.stream()
                .collect(Collectors.groupingBy(sale -> {
                    // Convert the sale's UTC OffsetDateTime to ZonedDateTime in the client's
                    // timezone
                    ZonedDateTime zonedSaleDate = sale.getDate().atZoneSameInstant(clientZoneOffset);
                    System.out.println("REPORT_SERVICE: Sale ID: " + sale.getId() +
                            ", UTC Date: " + sale.getDate() +
                            ", Zoned Date (" + clientZoneOffset + "): " + zonedSaleDate +
                            ", DayOfWeek: " + zonedSaleDate.getDayOfWeek());
                    return zonedSaleDate.getDayOfWeek(); // Get DayOfWeek in client's timezone
                }, Collectors.summingDouble(sale -> sale.getCash() + sale.getDigital())));

        cachedWeeklySales = weeklySalesMap.entrySet().stream()
                .map(entry -> new WeeklySalesData(entry.getKey().toString().substring(0, 3), entry.getValue()))
                .sorted(Comparator
                        .comparingInt(data -> DayOfWeek.valueOf(data.getDay().toUpperCase(Locale.ROOT)).getValue()))
                .collect(Collectors.toList());
        System.out.println("REPORT_SERVICE: Generated weekly sales data: " + cachedWeeklySales.size() + " entries.");

        // --- Monthly Sales Aggregation ---
        // Get the current ZonedDateTime in the client's timezone for month comparison
        ZonedDateTime currentClientZonedTime = ZonedDateTime.now(clientZoneOffset);
        System.out.println("REPORT_SERVICE: Current client zoned time for monthly filter: " + currentClientZonedTime);

        Map<Integer, Double> monthlySalesByWeekMap = salesList.stream()
                .filter(sale -> {
                    // Convert the sale's UTC OffsetDateTime to ZonedDateTime in the client's
                    // timezone
                    ZonedDateTime zonedSaleDate = sale.getDate().atZoneSameInstant(clientZoneOffset);
                    // Filter for current month in client's timezone based on the
                    // currentClientZonedTime
                    boolean isInCurrentMonth = zonedSaleDate.getMonth() == currentClientZonedTime.getMonth();
                    System.out.println("REPORT_SERVICE: Sale ID: " + sale.getId() +
                            ", Zoned Sale Month: " + zonedSaleDate.getMonth() +
                            ", Current Client Zoned Month: " + currentClientZonedTime.getMonth() +
                            ", Is in current month: " + isInCurrentMonth);
                    return isInCurrentMonth;
                })
                .collect(Collectors.groupingBy(sale -> {
                    // Convert the sale's UTC OffsetDateTime to ZonedDateTime in the client's
                    // timezone
                    ZonedDateTime zonedSaleDate = sale.getDate().atZoneSameInstant(clientZoneOffset);
                    WeekFields weekFields = WeekFields.of(Locale.getDefault());
                    int weekOfMonth = zonedSaleDate.get(weekFields.weekOfMonth()); // Get week of month in client's
                                                                                   // timezone
                    System.out.println("REPORT_SERVICE: Sale ID: " + sale.getId() + ", Week of Month in client zone: "
                            + weekOfMonth);
                    return weekOfMonth;
                }, Collectors.summingDouble(sale -> sale.getCash() + sale.getDigital())));

        cachedMonthlySales = monthlySalesByWeekMap.entrySet().stream()
                .map(entry -> new MonthlySalesData("W" + entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(data -> Integer.parseInt(data.getWeek().substring(1))))
                .collect(Collectors.toList());
        System.out.println("REPORT_SERVICE: Generated monthly sales data: " + cachedMonthlySales.size() + " entries.");

        System.out.println("--- Report generation completed ---");
    }

    @Override
    public List<WeeklySalesData> getWeeklySalesData() {
        if (cachedWeeklySales == null || cachedWeeklySales.isEmpty()) {
            System.out.println("REPORT_SERVICE: Weekly sales cache is empty, attempting to generate reports.");
            // Default to UTC (0 offset) if not explicitly passed when calling this method
            fetchAndGenerateReports(0);
        }
        return cachedWeeklySales;
    }

    @Override
    public List<MonthlySalesData> getMonthlySalesData() {
        if (cachedMonthlySales == null || cachedMonthlySales.isEmpty()) {
            System.out.println("REPORT_SERVICE: Monthly sales cache is empty, attempting to generate reports.");
            // Default to UTC (0 offset) if not explicitly passed when calling this method
            fetchAndGenerateReports(0);
        }
        return cachedMonthlySales;
    }

    private ReportDTO convertToDTO(ReportEntry entry) {
        return ReportDTO.builder()
                .itemName(entry.getItemName())
                .category(entry.getCategory())
                .quantitySold(entry.getQuantitySold())
                .revenue(entry.getTotalRevenue())
                .remainingStock(entry.getRemainingStock())
                .build();
    }
}
