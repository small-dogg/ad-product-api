package com.smalldogg.adproductapi.product.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "product",
        indexes = {
                @Index(name = "idx_product_partner_id", columnList = "partner_id"),
                @Index(name = "idx_product_partner_status", columnList = "partner_id,status"),
                @Index(name = "idx_product_created_at", columnList = "created_at")
        }
)
public class Product {

    /**
     * 목데이터 생성 시 직접 채우는 값이므로
     * @GeneratedValue 사용하지 않음
     */
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "partner_id", nullable = false)
    private Long partnerId;

    @Column(name = "category1", nullable = false)
    private Integer category1;

    @Column(name = "category2", nullable = false)
    private Integer category2;

    @Column(name = "category3", nullable = false)
    private Integer category3;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /**
     * ENUM + STRING 매핑
     * (ordinal 절대 사용 금지)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ProductStatus status;

    /**
     * 금액: 원 단위
     * 100 ~ 100,000,000
     */
    @Column(name = "price", nullable = false)
    private Long price;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "modified_at", nullable = false)
    private LocalDateTime modifiedAt;

    /* =========================
       Factory (목데이터 생성용)
       ========================= */
    public static Product of(
            Long id,
            Long partnerId, int category,
            String name,
            ProductStatus status,
            Long price,
            String imageUrl,
            LocalDateTime createdAt,
            LocalDateTime modifiedAt
    ) {
        Product product = new Product();
        product.id = id;
        product.partnerId = partnerId;
        product.category1 = (category / 100) * 100;
        product.category2 = ((category / 10) % 10) * 10;
        product.category3 = category % 10;
        product.name = name;
        product.status = status;
        product.price = price;
        product.imageUrl = imageUrl;
        product.createdAt = createdAt;
        product.modifiedAt = modifiedAt;
        return product;
    }

    /* =========================
       상태 변경 예시 (운영 대비)
       ========================= */
    public void changeStatus(ProductStatus status) {
        this.status = status;
        this.modifiedAt = LocalDateTime.now();
    }
}
