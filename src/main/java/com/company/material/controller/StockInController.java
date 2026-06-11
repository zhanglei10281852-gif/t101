package com.company.material.controller;

import com.company.material.entity.StockIn;
import com.company.material.entity.StockInItem;
import com.company.material.repository.StockInRepository;
import com.company.material.service.StockInService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stock-in")
@RequiredArgsConstructor
public class StockInController {

    private final StockInService stockInService;
    private final StockInRepository stockInRepository;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody StockInCreateRequest req, HttpServletRequest request) {
        try {
            String handler = (String) request.getAttribute("username");
            StockIn stockIn = new StockIn();
            stockIn.setStockInType(req.getStockInType());
            stockIn.setSupplierCode(req.getSupplierCode());
            stockIn.setWarehouseCode(req.getWarehouseCode());
            stockIn.setRemark(req.getRemark());
            stockIn.setStockInDate(req.getStockInDate());

            StockIn result = stockInService.createStockIn(stockIn, req.getItems(), handler);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String stockInType,
            @RequestParam(required = false) String keyword) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<StockIn> result;
        if (keyword != null && !keyword.isBlank()) {
            result = stockInRepository.search(keyword, pr);
        } else if (status != null && !status.isBlank()) {
            result = stockInRepository.findByStatus(status, pr);
        } else if (warehouseCode != null && !warehouseCode.isBlank()) {
            result = stockInRepository.findByWarehouseCode(warehouseCode, pr);
        } else if (stockInType != null && !stockInType.isBlank()) {
            result = stockInRepository.findByStockInType(stockInType, pr);
        } else {
            result = stockInRepository.findAll(pr);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return stockInRepository.findByIdWithItems(id)
                .map(s -> ResponseEntity.ok((Object) s))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/audit")
    public ResponseEntity<?> audit(@PathVariable Long id, HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"管理员".equals(role) && !"库管员".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "需要库管员或管理员角色才能审核"));
        }
        try {
            String auditor = (String) request.getAttribute("username");
            StockIn result = stockInService.auditStockIn(id, auditor);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/void")
    public ResponseEntity<?> voidStockIn(@PathVariable Long id, HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"管理员".equals(role) && !"库管员".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "需要库管员或管理员角色才能作废"));
        }
        try {
            String operator = (String) request.getAttribute("username");
            StockIn result = stockInService.voidStockIn(id, operator);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Data
    public static class StockInCreateRequest {
        private String stockInType;
        private String supplierCode;
        private String warehouseCode;
        private String remark;
        private java.time.LocalDate stockInDate;
        private List<StockInItem> items;
    }
}
