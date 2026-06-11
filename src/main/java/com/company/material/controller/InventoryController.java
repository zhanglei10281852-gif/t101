package com.company.material.controller;

import com.company.material.entity.Inventory;
import com.company.material.repository.InventoryRepository;
import com.company.material.repository.MaterialRepository;
import com.company.material.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryRepository inventoryRepository;
    private final MaterialRepository materialRepository;
    private final WarehouseRepository warehouseRepository;

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String materialCode,
            @RequestParam(required = false) String warehouseCode) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<Inventory> result;
        if (materialCode != null && !materialCode.isBlank()) {
            result = inventoryRepository.findPageByMaterialCode(materialCode, pr);
        } else if (warehouseCode != null && !warehouseCode.isBlank()) {
            result = inventoryRepository.findPageByWarehouseCode(warehouseCode, pr);
        } else {
            result = inventoryRepository.findAll(pr);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/warehouse/{warehouseCode}")
    public ResponseEntity<?> getByWarehouse(@PathVariable String warehouseCode) {
        List<Inventory> list = inventoryRepository.findByWarehouseCode(warehouseCode);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/material/{materialCode}")
    public ResponseEntity<?> getByMaterial(@PathVariable String materialCode) {
        List<Inventory> list = inventoryRepository.findByMaterialCode(materialCode);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/detail")
    public ResponseEntity<?> getDetail(
            @RequestParam String materialCode,
            @RequestParam String warehouseCode) {
        return inventoryRepository.findByMaterialCodeAndWarehouseCode(materialCode, warehouseCode)
                .map(inv -> ResponseEntity.ok((Object) inv))
                .orElse(ResponseEntity.notFound().build());
    }
}
