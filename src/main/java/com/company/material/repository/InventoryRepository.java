package com.company.material.repository;

import com.company.material.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByMaterialCodeAndWarehouseCode(String materialCode, String warehouseCode);
    List<Inventory> findByWarehouseCode(String warehouseCode);
    List<Inventory> findByMaterialCode(String materialCode);

    @Query("SELECT i FROM Inventory i WHERE (:materialCode IS NULL OR i.materialCode = :materialCode)")
    Page<Inventory> findPageByMaterialCode(@Param("materialCode") String materialCode, Pageable pageable);

    @Query("SELECT i FROM Inventory i WHERE (:warehouseCode IS NULL OR i.warehouseCode = :warehouseCode)")
    Page<Inventory> findPageByWarehouseCode(@Param("warehouseCode") String warehouseCode, Pageable pageable);

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Inventory i")
    BigDecimal sumTotalAmount();

    @Query("SELECT i.warehouseCode, COALESCE(SUM(i.totalAmount), 0) FROM Inventory i GROUP BY i.warehouseCode")
    List<Object[]> sumTotalAmountByWarehouse();
}
