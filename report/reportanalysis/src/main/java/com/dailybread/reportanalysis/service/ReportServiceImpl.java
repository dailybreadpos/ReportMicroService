package com.dailybread.reportanalysis.service;

import com.dailybread.reportanalysis.dto.ReportDTO;
import com.dailybread.reportanalysis.entity.ReportEntry;
import com.dailybread.reportanalysis.repository.ReportRepository;
import com.dailybread.reportanalysis.exception.ResourceNotFoundException;
import com.dailybread.reportanalysis.feign.InventoryClient;
import com.dailybread.reportanalysis.feign.PosClient;
import com.dailybread.reportanalysis.dto.Inventory;
import com.dailybread.reportanalysis.dto.Sales;
import com.dailybread.reportanalysis.dto.SaleItem;
import com.dailybread.reportanalysis.dto.WeeklySalesData; // New import
import com.dailybread.reportanalysis.dto.MonthlySalesData; // New import
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList; // New import
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final PosClient posClient;
    private final InventoryClient inventoryClient;

    // In-memory storage for aggregated sales data (for demonstration)
    // In a production app, this might be stored in the DB or re-calculated on
    // demand
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

    @Transactional
    @Override
    public void fetchAndGenerateReports() {
        // Fetch data from POS Microservice
        List<Sales> salesList = posClient.getAllSales();

        // Fetch data from Inventory Microservice
        List<Inventory> inventoryList = inventoryClient.getAllInventory();

        // Map inventory items by ID for easy lookup
        Map<Long, Inventory> inventoryMap = inventoryList.stream()
                .collect(Collectors.toMap(Inventory::getId, inventory -> inventory));

        // Clear old reports for fresh generation
        reportRepository.deleteAll();

        // Aggregate sales data and enrich with product details from inventory
        Map<String, Double> itemRevenue = salesList.stream()
                .flatMap(sales -> sales.getItems().stream())
                .collect(Collectors.groupingBy(item -> {
                    Inventory inv = inventoryMap.get(item.getItemId());
                    return inv != null ? inv.getName() : "Unknown Product (ID: " + item.getItemId() + ")";
                }, Collectors.summingDouble(item -> {
                    Inventory inv = inventoryMap.get(item.getItemId());
                    double price = (inv != null) ? inv.getPrice() : 0.0; // Use actual price from inventory
                    return item.getQuantity() * price;
                })));

        Map<String, Integer> itemQuantitySold = salesList.stream()
                .flatMap(sales -> sales.getItems().stream())
                .collect(Collectors.groupingBy(item -> {
                    Inventory inv = inventoryMap.get(item.getItemId());
                    return inv != null ? inv.getName() : "Unknown Product (ID: " + item.getItemId() + ")";
                }, Collectors.summingInt(SaleItem::getQuantity)));

        itemRevenue.forEach((itemName, totalRevenue) -> {
            Integer quantitySold = itemQuantitySold.getOrDefault(itemName, 0);
            // Find the associated Inventory object by name to get its stock and category
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
        });

        // Aggregate weekly sales data and cache it
        Map<DayOfWeek, Double> weeklySalesMap = salesList.stream()
                .collect(Collectors.groupingBy(sale -> sale.getDate().getDayOfWeek(),
                        Collectors.summingDouble(sale -> sale.getCash() + sale.getDigital())));

        cachedWeeklySales = weeklySalesMap.entrySet().stream()
                .map(entry -> new WeeklySalesData(entry.getKey().toString().substring(0, 3), entry.getValue())) // Shorten
                                                                                                                // day
                                                                                                                // name
                .sorted(Comparator
                        .comparingInt(data -> DayOfWeek.valueOf(data.getDay().toUpperCase(Locale.ROOT)).getValue())) // Sort
                                                                                                                     // by
                                                                                                                     // day
                                                                                                                     // of
                                                                                                                     // week
                .collect(Collectors.toList());

        // Aggregate monthly sales data by week of month and cache it
        Map<Integer, Double> monthlySalesByWeekMap = salesList.stream()
                .filter(sale -> sale.getDate().getMonth() == LocalDateTime.now().getMonth()) // Filter for current month
                .collect(Collectors.groupingBy(sale -> {
                    WeekFields weekFields = WeekFields.of(Locale.getDefault());
                    return sale.getDate().get(weekFields.weekOfMonth());
                }, Collectors.summingDouble(sale -> sale.getCash() + sale.getDigital())));

        cachedMonthlySales = monthlySalesByWeekMap.entrySet().stream()
                .map(entry -> new MonthlySalesData("W" + entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(data -> Integer.parseInt(data.getWeek().substring(1)))) // Sort by week
                                                                                                        // number
                .collect(Collectors.toList());
    }

    @Override
    public List<WeeklySalesData> getWeeklySalesData() {
        // Ensure data is generated before returning
        if (cachedWeeklySales == null || cachedWeeklySales.isEmpty()) {
            fetchAndGenerateReports(); // Re-generate if not available
        }
        return cachedWeeklySales;
    }

    @Override
    public List<MonthlySalesData> getMonthlySalesData() {
        // Ensure data is generated before returning
        if (cachedMonthlySales == null || cachedMonthlySales.isEmpty()) {
            fetchAndGenerateReports(); // Re-generate if not available
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
