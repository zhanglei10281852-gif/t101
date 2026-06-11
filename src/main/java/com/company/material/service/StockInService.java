package com.company.material.service;

import com.company.material.entity.*;
import com.company.material.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StockInService {

    private final StockInRepository stockInRepository;
    private final StockInItemRepository stockInItemRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final MaterialRepository materialRepository;
    private final WarehouseRepository warehouseRepository;
    private final SupplierRepository supplierRepository;

    @Transactional
    public StockIn createStockIn(StockIn stockIn, List<StockInItem> items, String handler) {
        if (stockIn.getStockInType() == null) {
            throw new IllegalArgumentException("入库类型不能为空");
        }
        if (stockIn.getWarehouseCode() == null) {
            throw new IllegalArgumentException("目标仓库不能为空");
        }
        if (!warehouseRepository.existsByWarehouseCode(stockIn.getWarehouseCode())) {
            throw new IllegalArgumentException("目标仓库不存在");
        }
        if ("采购入库".equals(stockIn.getStockInType())) {
            if (stockIn.getSupplierCode() == null) {
                throw new IllegalArgumentException("采购入库时供应商为必填");
            }
            if (!supplierRepository.existsBySupplierCode(stockIn.getSupplierCode())) {
                throw new IllegalArgumentException("供应商不存在");
            }
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("入库明细不能为空");
        }

        String stockInNo = generateStockInNo();
        stockIn.setStockInNo(stockInNo);
        stockIn.setHandler(handler);
        stockIn.setStatus("待审核");

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (StockInItem item : items) {
            if (item.getMaterialCode() == null) {
                throw new IllegalArgumentException("物料编码不能为空");
            }
            if (!materialRepository.existsByMaterialCode(item.getMaterialCode())) {
                throw new IllegalArgumentException("物料不存在: " + item.getMaterialCode());
            }
            if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("入库数量必须大于0");
            }
            if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("单价不能为空且不能为负");
            }
            item.setAmount(item.getQuantity().multiply(item.getUnitPrice()));
            totalAmount = totalAmount.add(item.getAmount());
            item.setStockIn(stockIn);
        }
        stockIn.setTotalAmount(totalAmount);
        stockIn.setItems(items);

        return stockInRepository.save(stockIn);
    }

    @Transactional
    public StockIn auditStockIn(Long id, String auditor) {
        StockIn stockIn = stockInRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("入库单不存在"));

        if (!"待审核".equals(stockIn.getStatus())) {
            throw new IllegalStateException("只有待审核状态的入库单才能审核");
        }

        List<StockInItem> items = stockInItemRepository.findByStockInId(id);
        String warehouseCode = stockIn.getWarehouseCode();

        for (StockInItem item : items) {
            String materialCode = item.getMaterialCode();
            BigDecimal inQty = item.getQuantity();
            BigDecimal inAmount = item.getAmount();

            Optional<Inventory> optInv = inventoryRepository.findByMaterialCodeAndWarehouseCode(materialCode, warehouseCode);
            Inventory inventory;
            BigDecimal beforeQty;

            if (optInv.isPresent()) {
                inventory = optInv.get();
                beforeQty = inventory.getQuantity();
                BigDecimal newQty = beforeQty.add(inQty);
                BigDecimal newTotalAmount = inventory.getTotalAmount().add(inAmount);
                BigDecimal newUnitCost = newQty.compareTo(BigDecimal.ZERO) > 0
                        ? newTotalAmount.divide(newQty, 4, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

                inventory.setQuantity(newQty);
                inventory.setTotalAmount(newTotalAmount);
                inventory.setUnitCost(newUnitCost);
            } else {
                inventory = new Inventory();
                inventory.setMaterialCode(materialCode);
                inventory.setWarehouseCode(warehouseCode);
                beforeQty = BigDecimal.ZERO;
                inventory.setQuantity(inQty);
                inventory.setTotalAmount(inAmount);
                inventory.setUnitCost(inQty.compareTo(BigDecimal.ZERO) > 0
                        ? inAmount.divide(inQty, 4, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO);
            }
            inventory.setLastStockInAt(LocalDateTime.now());
            inventoryRepository.save(inventory);

            InventoryLog log = new InventoryLog();
            log.setMaterialCode(materialCode);
            log.setWarehouseCode(warehouseCode);
            log.setChangeType("入库");
            log.setChangeQuantity(inQty);
            log.setBeforeQuantity(beforeQty);
            log.setAfterQuantity(inventory.getQuantity());
            log.setRelatedBillNo(stockIn.getStockInNo());
            log.setOperator(auditor);
            inventoryLogRepository.save(log);
        }

        stockIn.setStatus("已入库");
        stockIn.setAuditedAt(LocalDateTime.now());
        stockIn.setAuditor(auditor);
        return stockInRepository.save(stockIn);
    }

    @Transactional
    public StockIn voidStockIn(Long id, String operator) {
        StockIn stockIn = stockInRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("入库单不存在"));

        if (!"待审核".equals(stockIn.getStatus())) {
            throw new IllegalStateException("只有待审核状态的入库单才能作废");
        }

        stockIn.setStatus("已作废");
        return stockInRepository.save(stockIn);
    }

    private String generateStockInNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "RK" + dateStr;
        String maxNo = stockInRepository.findMaxStockInNoByPrefix(prefix);
        int seq = 1;
        if (maxNo != null && maxNo.length() == prefix.length() + 4) {
            try {
                seq = Integer.parseInt(maxNo.substring(prefix.length())) + 1;
            } catch (Exception ignored) {
            }
        }
        return prefix + String.format("%04d", seq);
    }
}
