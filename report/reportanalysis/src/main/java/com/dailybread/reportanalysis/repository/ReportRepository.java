package com.dailybread.reportanalysis.repository;

import com.dailybread.reportanalysis.entity.ReportEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<ReportEntry, Long> {
}
