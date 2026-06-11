package com.company.material.repository;

import com.company.material.entity.InventoryLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {
    List<InventoryLog> findByMaterialCodeAndWarehouseCodeOrderByOperateTimeDesc(String materialCode, String warehouseCode);
    Page<InventoryLog> findByMaterialCode(String materialCode, Pageable pageable);
    Page<InventoryLog> findByWarehouseCode(String warehouseCode, Pageable pageable);
    Page<InventoryLog> findByRelatedBillNo(String relatedBillNo, Pageable pageable);
}
