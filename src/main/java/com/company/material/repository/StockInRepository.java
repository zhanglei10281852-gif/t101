package com.company.material.repository;

import com.company.material.entity.StockIn;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface StockInRepository extends JpaRepository<StockIn, Long> {
    Optional<StockIn> findByStockInNo(String stockInNo);
    boolean existsByStockInNo(String stockInNo);
    Page<StockIn> findByStatus(String status, Pageable pageable);
    Page<StockIn> findByWarehouseCode(String warehouseCode, Pageable pageable);
    Page<StockIn> findByStockInType(String stockInType, Pageable pageable);

    @Query("SELECT s FROM StockIn s WHERE s.stockInNo LIKE %:kw% OR s.remark LIKE %:kw%")
    Page<StockIn> search(@Param("kw") String kw, Pageable pageable);

    @Query("SELECT COUNT(s) FROM StockIn s WHERE s.stockInDate BETWEEN :start AND :end")
    long countByDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM StockIn s WHERE s.stockInDate BETWEEN :start AND :end AND s.status = '已入库'")
    java.math.BigDecimal sumAmountByDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT MAX(s.stockInNo) FROM StockIn s WHERE s.stockInNo LIKE :prefix%")
    String findMaxStockInNoByPrefix(@Param("prefix") String prefix);

    @Query("SELECT s FROM StockIn s LEFT JOIN FETCH s.items WHERE s.id = :id")
    Optional<StockIn> findByIdWithItems(@Param("id") Long id);
}
