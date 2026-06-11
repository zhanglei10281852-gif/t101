package com.company.material.controller;

import com.company.material.entity.InventoryLog;
import com.company.material.repository.InventoryLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory-logs")
@RequiredArgsConstructor
public class InventoryLogController {

    private final InventoryLogRepository inventoryLogRepository;

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String materialCode,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String relatedBillNo) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("operateTime").descending());
        Page<InventoryLog> result;
        if (materialCode != null && !materialCode.isBlank()) {
            result = inventoryLogRepository.findByMaterialCode(materialCode, pr);
        } else if (warehouseCode != null && !warehouseCode.isBlank()) {
            result = inventoryLogRepository.findByWarehouseCode(warehouseCode, pr);
        } else if (relatedBillNo != null && !relatedBillNo.isBlank()) {
            result = inventoryLogRepository.findByRelatedBillNo(relatedBillNo, pr);
        } else {
            result = inventoryLogRepository.findAll(pr);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/detail")
    public ResponseEntity<?> getByMaterialAndWarehouse(
            @RequestParam String materialCode,
            @RequestParam String warehouseCode) {
        List<InventoryLog> list = inventoryLogRepository
                .findByMaterialCodeAndWarehouseCodeOrderByOperateTimeDesc(materialCode, warehouseCode);
        return ResponseEntity.ok(list);
    }
}
