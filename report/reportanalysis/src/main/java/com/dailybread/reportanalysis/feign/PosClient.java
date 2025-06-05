package com.dailybread.reportanalysis.feign;

import com.dailybread.reportanalysis.dto.GetSaleResponse;
import com.dailybread.reportanalysis.dto.Sales;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

// Relying on Eureka service ID
@FeignClient(name = "POSMICROSERVICE") // Use the exact service ID from GatewayConfig
public interface PosClient {

    @GetMapping("/api/pos/sales")
    List<Sales> getAllSales();

    @GetMapping("/api/pos/saless")
    List<GetSaleResponse> getAllSaleResponses();
}
