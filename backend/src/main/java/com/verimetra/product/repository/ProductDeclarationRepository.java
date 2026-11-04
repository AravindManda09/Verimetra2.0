package com.verimetra.product.repository;

import com.verimetra.product.entity.ProductDeclaration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductDeclarationRepository extends JpaRepository<ProductDeclaration, Long> {
    List<ProductDeclaration> findByProductId(Long productId);
    List<ProductDeclaration> findByProductIdAndSource(Long productId, String source);
    Optional<ProductDeclaration> findByProductIdAndSourceAndFieldName(Long productId, String source, String fieldName);
    
    @Query("SELECT pd FROM ProductDeclaration pd WHERE pd.productId = :productId AND pd.source = :source AND pd.confirmed = true")
    List<ProductDeclaration> findConfirmedDeclarations(@Param("productId") Long productId, @Param("source") String source);
    
    @Query("SELECT pd FROM ProductDeclaration pd WHERE pd.productId = :productId AND pd.fieldName = :fieldName AND pd.confirmed = true")
    List<ProductDeclaration> findConfirmedByField(@Param("productId") Long productId, @Param("fieldName") String fieldName);
    
    void deleteByProductIdAndSource(Long productId, String source);
}
