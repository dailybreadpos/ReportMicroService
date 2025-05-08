package com.dailybread.reportanalysis.service;

import com.dailybread.reportanalysis.dto.ReportDTO;
import com.dailybread.reportanalysis.entity.ReportEntry;
import com.dailybread.reportanalysis.repository.ReportRepository;
import com.dailybread.reportanalysis.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;

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
    public void fetchAndGenerateReports() {
        // Simulate pulling from Inventory and POS services
        // You would normally use RestTemplate or FeignClient
        ReportEntry dummy = ReportEntry.builder()
                .itemName("Coke")
                .category("Beverages")
                .quantitySold(200)
                .totalRevenue(5000.0)
                .remainingStock(15)
                .build();

        reportRepository.save(dummy);
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
