package com.dailybread.reportanalysis.rest;

import com.dailybread.reportanalysis.dto.ReportDTO;
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
    public String generateReports() {
        reportService.fetchAndGenerateReports();
        return "Reports generated from Inventory and POS.";
    }
}
