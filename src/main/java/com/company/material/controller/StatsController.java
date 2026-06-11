package com.company.material.controller;

import com.company.material.entity.Inventory;
import com.company.material.entity.Material;
import com.company.material.repository.InventoryRepository;
import com.company.material.repository.MaterialRepository;
import com.company.material.repository.StockInRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final InventoryRepository inventoryRepository;
    private final StockInRepository stockInRepository;
    private final MaterialRepository materialRepository;

    @GetMapping("/inventory/total-amount")
    public ResponseEntity<?> getTotalInventoryAmount() {
        BigDecimal total = inventoryRepository.sumTotalAmount();
        return ResponseEntity.ok(Map.of("totalAmount", total));
    }

    @GetMapping("/inventory/by-warehouse")
    public ResponseEntity<?> getInventoryByWarehouse() {
        List<Object[]> list = inventoryRepository.sumTotalAmountByWarehouse();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : list) {
            Map<String, Object> item = new HashMap<>();
            item.put("warehouseCode", row[0]);
            item.put("totalAmount", row[1]);
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/inventory/by-category")
    public ResponseEntity<?> getInventoryByCategory() {
        List<Material> materials = materialRepository.findAll();
        Map<String, String> materialCategoryMap = materials.stream()
                .collect(Collectors.toMap(Material::getMaterialCode, Material::getCategory, (a, b) -> a));

        List<Inventory> allInventory = inventoryRepository.findAll();
        Map<String, BigDecimal> categoryQtyMap = new HashMap<>();
        for (Inventory inv : allInventory) {
            String category = materialCategoryMap.get(inv.getMaterialCode());
            if (category == null) category = "未分类";
            BigDecimal qty = inv.getQuantity() == null ? BigDecimal.ZERO : inv.getQuantity();
            categoryQtyMap.merge(category, qty, BigDecimal::add);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : categoryQtyMap.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("category", entry.getKey());
            item.put("totalQuantity", entry.getValue());
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stock-in/monthly")
    public ResponseEntity<?> getStockInMonthly(
            @RequestParam(required = false) Integer year) {
        if (year == null) {
            year = LocalDate.now().getYear();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            YearMonth ym = YearMonth.of(year, month);
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();
            long count = stockInRepository.countByDateRange(start, end);
            BigDecimal amount = stockInRepository.sumAmountByDateRange(start, end);
            Map<String, Object> item = new HashMap<>();
            item.put("year", year);
            item.put("month", month);
            item.put("count", count);
            item.put("amount", amount);
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/safety-stock/count")
    public ResponseEntity<?> getSafetyStockAlertCount() {
        List<Material> materials = materialRepository.findAll();
        Map<String, Material> materialMap = materials.stream()
                .collect(Collectors.toMap(Material::getMaterialCode, m -> m));

        List<Inventory> allInventory = inventoryRepository.findAll();
        int belowCount = 0;
        int outOfStockCount = 0;
        Set<String> processed = new HashSet<>();

        for (Inventory inv : allInventory) {
            Material m = materialMap.get(inv.getMaterialCode());
            if (m == null || m.getSafetyStock() == null) continue;
            String key = inv.getMaterialCode() + "_" + inv.getWarehouseCode();
            if (processed.contains(key)) continue;
            processed.add(key);

            BigDecimal safetyStock = BigDecimal.valueOf(m.getSafetyStock());
            BigDecimal qty = inv.getQuantity() == null ? BigDecimal.ZERO : inv.getQuantity();

            if (qty.compareTo(BigDecimal.ZERO) == 0) {
                outOfStockCount++;
            } else if (qty.compareTo(safetyStock) < 0) {
                belowCount++;
            }
        }

        for (Material m : materials) {
            if (m.getSafetyStock() != null && m.getSafetyStock() > 0) {
                boolean found = allInventory.stream()
                        .anyMatch(inv -> inv.getMaterialCode().equals(m.getMaterialCode()));
                if (!found) {
                    outOfStockCount++;
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("belowSafetyCount", belowCount);
        result.put("outOfStockCount", outOfStockCount);
        result.put("totalAlertCount", belowCount + outOfStockCount);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/overview")
    public ResponseEntity<?> getOverview() {
        Map<String, Object> result = new HashMap<>();

        BigDecimal totalInvAmount = inventoryRepository.sumTotalAmount();
        result.put("totalInventoryAmount", totalInvAmount);

        long materialCount = materialRepository.count();
        result.put("materialCount", materialCount);

        List<Inventory> allInv = inventoryRepository.findAll();
        result.put("inventoryRecordCount", allInv.size());

        long stockInCount = stockInRepository.count();
        result.put("stockInCount", stockInCount);

        return ResponseEntity.ok(result);
    }
}
