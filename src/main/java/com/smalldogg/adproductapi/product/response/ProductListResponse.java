package com.smalldogg.adproductapi.product.response;

import java.time.LocalDateTime;
import java.util.List;

public record ProductListResponse(
        List<ProductItem> items,
        PageMeta page
) {
    public record ProductItem(
            Long id,
            Long partnerId,
            Integer category1,
            Integer category2,
            Integer category3,
            String name,
            String status,
            Long price,
            String imageUrl,
            LocalDateTime createdAt,
            LocalDateTime modifiedAt
    ) {}

    public record PageMeta(
            int page,
            int limit,
            long totalElements,
            int totalPages,
            boolean hasNext
    ) {}
}
