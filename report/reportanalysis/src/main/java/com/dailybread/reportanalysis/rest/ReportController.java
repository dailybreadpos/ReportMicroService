package com.dailybread.reportanalysis.rest;

import com.dailybread.reportanalysis.dto.ReportDTO;
import com.dailybread.reportanalysis.dto.WeeklySalesData;
import com.dailybread.reportanalysis.dto.MonthlySalesData;
import com.dailybread.reportanalysis.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    public List<ReportDTO> getAllReports() {
        return reportService.getAllReports();
    }

    @GetMapping("/{item}")
    public ReportDTO getReportByItem(@PathVariable String item) {
        return reportService.getReportByItem(item);
    }

    @PostMapping("/generate")
    public String generateReports(
            @RequestParam(required = false, defaultValue = "0") Integer offsetHours) {
        // CORRECTED: Pass the offsetHours parameter to the service method
        reportService.fetchAndGenerateReports(offsetHours);
        return "Reports generated from Inventory and POS.";
    }

    @GetMapping("/weekly")
    public List<WeeklySalesData> getWeeklySales() {
        return reportService.getWeeklySalesData();
    }

    @GetMapping("/monthly")
    public List<MonthlySalesData> getMonthlySales() {
        return reportService.getMonthlySalesData();
    }
}
