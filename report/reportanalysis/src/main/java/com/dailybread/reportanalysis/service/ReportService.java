package com.dailybread.reportanalysis.service;

import com.dailybread.reportanalysis.dto.ReportDTO;
import java.util.List;

public interface ReportService {
    List<ReportDTO> getAllReports();

    ReportDTO getReportByItem(String itemName);

    void fetchAndGenerateReports(); // Fetch from Inventory + POS
}
