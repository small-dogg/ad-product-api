package com.smalldogg.adproductapi.product.response;

import java.time.LocalDateTime;

/**
 * Product data from external Product API.
 */
public record ProductApiResponse(
        Long id,
        String name,
        Long price,
        String imageUrl,
        Long partnerId,
        Integer category1,
        Integer category2,
        Integer category3,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {
}
