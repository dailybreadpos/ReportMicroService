package com.dailybread.reportanalysis.service;

import com.dailybread.reportanalysis.dto.ReportDTO;
import com.dailybread.reportanalysis.dto.WeeklySalesData;
import com.dailybread.reportanalysis.dto.MonthlySalesData;
import java.util.List;

public interface ReportService {
    List<ReportDTO> getAllReports();

    ReportDTO getReportByItem(String itemName);

    void fetchAndGenerateReports(Integer offsetHours); // Updated signature to accept offset

    List<WeeklySalesData> getWeeklySalesData(); // New method

    List<MonthlySalesData> getMonthlySalesData(); // New method
}
