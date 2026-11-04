package com.verimetra.product.repository;

import com.verimetra.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByCategory(String category, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE LOWER(p.productName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Product> searchProducts(@Param("search") String search, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.category = :category AND LOWER(p.productName) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Product> searchByCategory(@Param("category") String category, @Param("search") String search, Pageable pageable);
    
    List<Product> findByCategory(String category);
    
    @Query("SELECT p.category, COUNT(p) FROM Product p GROUP BY p.category")
    List<Object[]> countByCategory();
    
    List<Product> findByCategoryIn(List<String> categories);
    
    @Query("SELECT COUNT(p) FROM Product p")
    long countTotal();
}
