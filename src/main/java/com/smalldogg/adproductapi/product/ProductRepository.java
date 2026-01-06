package com.smalldogg.adproductapi.product;

import com.smalldogg.adproductapi.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByPartnerId(Long partnerId, Pageable pageable);

    Page<Product> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

    Page<Product> findByPartnerIdAndNameContainingIgnoreCase(Long partnerId, String keyword, Pageable pageable);
}
