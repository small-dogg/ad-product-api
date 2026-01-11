package com.smalldogg.adproductapi.product;

import com.smalldogg.adproductapi.config.ProductNotFoundException;
import com.smalldogg.adproductapi.product.entity.Product;
import com.smalldogg.adproductapi.product.entity.ProductStatus;
import com.smalldogg.adproductapi.product.enums.ProductSearchType;
import com.smalldogg.adproductapi.product.enums.ProductSortType;
import com.smalldogg.adproductapi.product.response.BulkAddResponse;
import com.smalldogg.adproductapi.product.response.ProductApiResponse;
import com.smalldogg.adproductapi.product.response.ProductListResponse;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@RequiredArgsConstructor
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final EntityManager entityManager;


    /**
     * Controller의 매개는 그대로(MultipartFile file).
     * upsert 인자는 시그니처에 남겨두되, JPA saveAll로는 DB 레벨 UPSERT를 직접 제어할 수 없으므로
     * 동작은 "ID가 있으면 update, 없으면 insert" (JPA merge 성격)로 이해하는 것이 안전합니다.
     */
    @Transactional
    public BulkAddResponse importCsv(MultipartFile file, boolean upsert) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("CSV file is empty.");
        }

        final int chunkSize = 1_000; // saveAll 청크 크기 (1000~3000 사이 권장)
        long totalLines = 0;
        long affected = 0;
        long skipped = 0;

        // 중복 ID가 CSV 내에 섞일 수 있으면, 같은 청크 내 dedup이 도움이 됨
        // (마지막 값으로 덮어쓰기)
        Map<Long, Product> buffer = new LinkedHashMap<>(chunkSize * 2);

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String header = br.readLine();
            if (header == null) return new BulkAddResponse(0, 0, 0);

            if (!header.toLowerCase().startsWith("id,partner_id,category")) {
                throw new IllegalArgumentException("Invalid CSV header. Expected: id,partner_id,category,...");
            }

            String line;
            while ((line = br.readLine()) != null) {
                totalLines++;

                if (line.isBlank()) {
                    skipped++;
                    continue;
                }

                // 생성기에서 name에 콤마를 넣지 않는 전제.
                // (콤마 가능성이 있으면 Commons CSV로 교체 권장)
                String[] t = line.split(",", 9);
                if (t.length != 9) {
                    skipped++;
                    continue;
                }

                try {
                    Long id = Long.parseLong(t[0].trim());
                    Long partnerId = Long.parseLong(t[1].trim());
                    int category = Integer.parseInt(t[2].trim());
                    String name = t[3].trim();
                    ProductStatus status = ProductStatus.valueOf(t[4].trim());
                    Long price = Long.parseLong(t[5].trim());
                    String imageUrl = emptyToNull(t[6].trim());

                    LocalDateTime createdAt = parseDateTime(t[7].trim());
                    LocalDateTime modifiedAt = parseDateTime(t[8].trim());

                    if (modifiedAt.isBefore(createdAt)) {
                        skipped++;
                        continue;
                    }

                    Product product = Product.of(
                            id, partnerId, category, name, status, price, imageUrl, createdAt, modifiedAt
                    );

                    // 같은 청크 내 중복 id는 마지막 행으로 덮어쓰기
                    buffer.put(id, product);

                } catch (Exception e) {
                    skipped++;
                    continue;
                }

                if (buffer.size() >= chunkSize) {
                    affected += flushChunk(buffer.values());
                    buffer.clear();
                }
            }

            if (!buffer.isEmpty()) {
                affected += flushChunk(buffer.values());
                buffer.clear();
            }
        }

        return new BulkAddResponse(totalLines, affected, skipped);
    }

    private long flushChunk(Collection<Product> products) {
        // saveAll은 내부적으로 persist/merge를 섞어 처리할 수 있으며,
        // "affected rows"를 정확히 알기 어렵습니다. 여기서는 처리 건수로 반환합니다.
        productRepository.saveAll(products);

        // 청크마다 flush/clear로 메모리 사용량과 dirty checking 부담을 낮춤
        productRepository.flush();
        entityManager.clear();

        return products.size();
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    /**
     * 허용 포맷:
     * - 2026-01-06T12:34:56
     * - 2026-01-06 12:34:56
     */
    private static LocalDateTime parseDateTime(String raw) {
        String v = raw.trim();
        if (v.contains("T")) {
            return LocalDateTime.parse(v);
        }
        return LocalDateTime.parse(v.replace(" ", "T"));
    }

    public ProductListResponse getProducts(int page, int limit, Long partnerId, ProductSortType sortType, String searchKeyword, ProductSearchType searchType) {
        int safePage = Math.max(page, 0);
        int safeLimit = Math.min(Math.max(limit, 1), 200); // limit 상한 (예: 200)

        Pageable pageable = PageRequest.of(safePage, safeLimit, toSort(sortType));

        boolean hasKeyword = searchKeyword != null && !searchKeyword.isBlank();
        String keyword = hasKeyword ? searchKeyword.trim() : null;

        Page<Product> result;
        if (!hasKeyword) {
            // 검색어 없음: partnerId 필터만 반영
            if (partnerId == null) {
                result = productRepository.findAll(pageable);
            } else {
                result = productRepository.findByPartnerId(partnerId, pageable);
            }
        } else {
            // 검색어 있음: searchType별로 분기 (현재는 NAME만 구현)
            switch (searchType) {
                case NAME -> {
                    if (partnerId == null) {
                        result = productRepository.findByNameContainingIgnoreCase(keyword, pageable);
                    } else {
                        result = productRepository.findByPartnerIdAndNameContainingIgnoreCase(partnerId, keyword, pageable);
                    }
                }
                default -> throw new IllegalArgumentException("Unsupported searchType: " + searchType);
            }
        }

        List<ProductListResponse.ProductItem> items = result.getContent().stream()
                .map(this::toItem)
                .toList();

        return new ProductListResponse(
                items,
                new ProductListResponse.PageMeta(
                        safePage,
                        safeLimit,
                        result.getTotalElements(),
                        result.getTotalPages(),
                        result.hasNext()
                )
        );
    }

    private Sort toSort(ProductSortType sortType) {
        return switch (sortType) {
            case ID -> Sort.by(Sort.Direction.ASC, "id");
            case ID_DESC -> Sort.by(Sort.Direction.DESC, "id");
            case CREATED_AT -> Sort.by(Sort.Direction.DESC, "createdAt");
            case PRICE_ASC -> Sort.by(Sort.Direction.ASC, "price");
            case PRICE_DESC -> Sort.by(Sort.Direction.DESC, "price");
        };
    }

    private ProductListResponse.ProductItem toItem(Product p) {
        return new ProductListResponse.ProductItem(
                p.getId(),
                p.getPartnerId(),
                p.getCategory1(),
                p.getCategory2(),
                p.getCategory3(),
                p.getName(),
                p.getStatus().name(),
                p.getPrice(),
                p.getImageUrl(),
                p.getCreatedAt(),
                p.getModifiedAt()
        );
    }

    @Transactional(readOnly = true)
    public ProductApiResponse getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        return new ProductApiResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getImageUrl(),
                product.getPartnerId(),
                product.getCategory1(),
                product.getCategory2(),
                product.getCategory3(),
                product.getCreatedAt(),
                product.getModifiedAt()
        );
    }
}
