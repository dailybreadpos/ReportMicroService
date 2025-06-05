package com.dailybread.reportanalysis.feign;

import com.dailybread.reportanalysis.dto.Inventory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "INVENTORY")
public interface InventoryClient {

    @GetMapping("/api/inventory")
    List<Inventory> getAllInventory();
}
