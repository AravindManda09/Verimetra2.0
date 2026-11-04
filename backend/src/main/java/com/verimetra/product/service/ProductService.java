package com.verimetra.product.service;

import com.verimetra.common.exception.ResourceNotFoundException;
import com.verimetra.normalization.NormalizationService;
import com.verimetra.product.dto.*;
import com.verimetra.product.entity.Product;
import com.verimetra.product.entity.ProductDeclaration;
import com.verimetra.product.repository.ProductDeclarationRepository;
import com.verimetra.product.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductDeclarationRepository declarationRepository;
    private final NormalizationService normalizationService;

    public ProductService(ProductRepository productRepository,
                         ProductDeclarationRepository declarationRepository,
                         NormalizationService normalizationService) {
        this.productRepository = productRepository;
        this.declarationRepository = declarationRepository;
        this.normalizationService = normalizationService;
    }

    @Transactional
    public ProductDTO createProduct(CreateProductRequest request) {
        Product product = new Product();
        product.setProductName(request.productName());
        product.setSku(request.sku());
        product.setCategory(request.category());
        product.setManufacturerName(request.manufacturerName());
        product.setCountryOfOrigin(request.countryOfOrigin());
        product.setDescription(request.description());
        product = productRepository.save(product);
        
        // Create listing declarations from product fields
        createListingDeclarations(product);
        
        return toDTO(product);
    }

    private void createListingDeclarations(Product product) {
        saveDeclaration(product.getId(), "LISTING", "product_name", product.getProductName());
        if (product.getManufacturerName() != null) {
            saveDeclaration(product.getId(), "LISTING", "manufacturer_name", product.getManufacturerName());
        }
        if (product.getCountryOfOrigin() != null) {
            saveDeclaration(product.getId(), "LISTING", "country_of_origin", product.getCountryOfOrigin());
        }
    }

    private void saveDeclaration(Long productId, String source, String fieldName, String value) {
        ProductDeclaration decl = new ProductDeclaration();
        decl.setProductId(productId);
        decl.setSource(source);
        decl.setFieldName(fieldName);
        decl.setFieldValue(value);
        decl.setConfirmed(true);
        decl.setConfirmedAt(Instant.now());
        
        NormalizationService.NormalizationResult norm = normalizationService.normalize(fieldName, value);
        decl.setNormalizedValue(norm.normalizedValue());
        decl.setNormalizationUnit(norm.unit());
        
        declarationRepository.save(decl);
    }

    public ProductDTO getProduct(Long id) {
        return toDTO(productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id)));
    }

    public Page<ProductDTO> listProducts(String category, String search, Pageable pageable) {
        Page<Product> products;
        if (category != null && search != null) {
            products = productRepository.searchByCategory(category, search, pageable);
        } else if (category != null) {
            products = productRepository.findByCategory(category, pageable);
        } else if (search != null) {
            products = productRepository.searchProducts(search, pageable);
        } else {
            products = productRepository.findAll(pageable);
        }
        return products.map(this::toDTO);
    }

    @Transactional
    public ProductDTO updateProduct(Long id, CreateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        product.setProductName(request.productName());
        product.setSku(request.sku());
        product.setCategory(request.category());
        product.setManufacturerName(request.manufacturerName());
        product.setCountryOfOrigin(request.countryOfOrigin());
        product.setDescription(request.description());
        product = productRepository.save(product);
        return toDTO(product);
    }

    @Transactional
    public List<ProductDeclarationDTO> updateDeclarations(Long productId, UpdateDeclarationRequest request) {
        productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        
        for (UpdateDeclarationRequest.DeclarationFieldRequest field : request.fields()) {
            ProductDeclaration decl = declarationRepository
                    .findByProductIdAndSourceAndFieldName(productId, request.source(), field.fieldName())
                    .orElseGet(() -> {
                        ProductDeclaration newDecl = new ProductDeclaration();
                        newDecl.setProductId(productId);
                        newDecl.setSource(request.source());
                        newDecl.setFieldName(field.fieldName());
                        return newDecl;
                    });
            
            decl.setFieldValue(field.fieldValue());
            decl.setConfirmed(true);
            decl.setConfirmedAt(Instant.now());
            
            NormalizationService.NormalizationResult norm = normalizationService.normalize(field.fieldName(), field.fieldValue());
            decl.setNormalizedValue(norm.normalizedValue());
            decl.setNormalizationUnit(norm.unit());
            
            declarationRepository.save(decl);
        }
        
        return getDeclarations(productId);
    }

    public List<ProductDeclarationDTO> getDeclarations(Long productId) {
        return declarationRepository.findByProductId(productId).stream()
                .map(this::toDeclDTO)
                .toList();
    }

    public List<Map<String, Object>> getProductsByCategory() {
        return productRepository.countByCategory().stream()
                .map(row -> Map.<String, Object>of("category", row[0], "count", row[1]))
                .toList();
    }

    private ProductDTO toDTO(Product p) {
        return new ProductDTO(p.getId(), p.getProductName(), p.getSku(), p.getCategory(),
                p.getManufacturerName(), p.getCountryOfOrigin(), p.getDescription(),
                p.getCreatedAt(), p.getUpdatedAt());
    }

    private ProductDeclarationDTO toDeclDTO(ProductDeclaration d) {
        return new ProductDeclarationDTO(d.getId(), d.getProductId(), d.getSource(),
                d.getFieldName(), d.getFieldValue(), d.getNormalizedValue(),
                d.getNormalizationUnit(), d.getConfirmed(), d.getConfirmedAt(), d.getCreatedAt());
    }
}
