package com.company.material.controller;

import com.company.material.entity.Inventory;
import com.company.material.entity.Material;
import com.company.material.repository.InventoryRepository;
import com.company.material.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/safety-stock")
@RequiredArgsConstructor
public class SafetyStockController {

    private final InventoryRepository inventoryRepository;
    private final MaterialRepository materialRepository;

    @GetMapping("/alerts")
    public ResponseEntity<?> getAlerts() {
        List<Material> materials = materialRepository.findAll();
        Map<String, Material> materialMap = materials.stream()
                .collect(Collectors.toMap(Material::getMaterialCode, m -> m));

        List<Inventory> allInventory = inventoryRepository.findAll();
        List<Map<String, Object>> alerts = new ArrayList<>();
        Set<String> processed = new HashSet<>();

        for (Inventory inv : allInventory) {
            Material m = materialMap.get(inv.getMaterialCode());
            if (m == null || m.getSafetyStock() == null) continue;

            String key = inv.getMaterialCode();
            BigDecimal safetyStock = BigDecimal.valueOf(m.getSafetyStock());
            BigDecimal qty = inv.getQuantity() == null ? BigDecimal.ZERO : inv.getQuantity();

            if (qty.compareTo(safetyStock) < 0) {
                Map<String, Object> alert = new HashMap<>();
                alert.put("materialCode", m.getMaterialCode());
                alert.put("materialName", m.getName());
                alert.put("category", m.getCategory());
                alert.put("unit", m.getUnit());
                alert.put("safetyStock", m.getSafetyStock());
                alert.put("currentStock", qty);
                alert.put("warehouseCode", inv.getWarehouseCode());
                if (qty.compareTo(BigDecimal.ZERO) == 0) {
                    alert.put("alertLevel", "断货预警");
                } else {
                    alert.put("alertLevel", "低于安全库存");
                }
                alerts.add(alert);
            }
            processed.add(key);
        }

        for (Material m : materials) {
            if (!processed.contains(m.getMaterialCode()) && m.getSafetyStock() != null && m.getSafetyStock() > 0) {
                Map<String, Object> alert = new HashMap<>();
                alert.put("materialCode", m.getMaterialCode());
                alert.put("materialName", m.getName());
                alert.put("category", m.getCategory());
                alert.put("unit", m.getUnit());
                alert.put("safetyStock", m.getSafetyStock());
                alert.put("currentStock", BigDecimal.ZERO);
                alert.put("warehouseCode", null);
                alert.put("alertLevel", "断货预警");
                alerts.add(alert);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", alerts.size());
        result.put("items", alerts);
        return ResponseEntity.ok(result);
    }
}
