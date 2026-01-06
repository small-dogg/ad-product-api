package com.smalldogg.adproductapi.mock;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.UUID;

public class ProductMockDataGenerator {

    enum ProductStatus {
        ACTIVE, REJECTED, SUSPENDED, SOLD_OUT
    }

    public static void main(String[] args) throws Exception {
        int totalProducts = 100_000;
        int partnerCount = 1_000;
        int maxProductsPerPartner = 1_000;

        // partnerId는 1..1000
        long partnerIdStart = 1L;

        Path out = Path.of("products.csv");
        generateCsv(out, totalProducts, partnerCount, maxProductsPerPartner, partnerIdStart);

        System.out.println("Generated: " + out.toAbsolutePath());
    }

    /**
     * 파트너당 capacity(최대 상품 수)를 두고,
     * "아직 capacity가 남아있는 파트너 목록"에서 랜덤으로 하나를 뽑아 배정합니다.
     * - 어떤 파트너는 0개가 될 수 있음
     * - 어떤 파트너는 최대 1000개까지 갈 수 있음
     * - 전체 합은 정확히 totalProducts
     */
    static void generateCsv(Path out,
                            int totalProducts,
                            int partnerCount,
                            int maxProductsPerPartner,
                            long partnerIdStart) throws IOException {

        SplittableRandom rnd = new SplittableRandom();

        // 남은 capacity
        int[] used = new int[partnerCount];

        // 아직 capacity가 남아있는 파트너 인덱스 풀(0..partnerCount-1)
        List<Integer> available = new ArrayList<>(partnerCount);
        for (int i = 0; i < partnerCount; i++) available.add(i);

        Instant now = Instant.now();
        Instant fiveYearsAgo = now.minusSeconds(60L * 60 * 24 * 365 * 5); // 단순 5년(윤년 무시)
        long startEpoch = fiveYearsAgo.getEpochSecond();
        long endEpoch = now.getEpochSecond();

        try (BufferedWriter w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            // header
            w.write("id,partner_id,name,status,price,image_url,created_at,modified_at");
            w.newLine();

            long id = 1L;
            for (int i = 0; i < totalProducts; i++, id++) {
                if (available.isEmpty()) {
                    throw new IllegalStateException("No available partners left. Check constraints.");
                }

                // 남은 파트너 풀에서 랜덤 선택
                int pickPos = rnd.nextInt(available.size());
                int partnerIdx = available.get(pickPos);

                long partnerId = partnerIdStart + partnerIdx;

                // capacity 사용량 증가, 꽉 차면 available에서 제거 (O(1) swap-remove)
                used[partnerIdx]++;
                if (used[partnerIdx] >= maxProductsPerPartner) {
                    int last = available.size() - 1;
                    available.set(pickPos, available.get(last));
                    available.remove(last);
                }

                ProductStatus status = randomStatus(rnd);
                long price = randomPrice(rnd, 100L, 100_000_000L);
                String imageUrl = "https://cdn.jerry.world/product/thumbnail/" + UUID.randomUUID();

                // createdAt: 5년 내 랜덤
                long createdEpoch = randomLong(rnd, startEpoch, endEpoch);
                // modifiedAt: createdAt ~ now
                long modifiedEpoch = randomLong(rnd, createdEpoch, endEpoch);

                LocalDateTime createdAt = LocalDateTime.ofInstant(Instant.ofEpochSecond(createdEpoch), ZoneId.systemDefault());
                LocalDateTime modifiedAt = LocalDateTime.ofInstant(Instant.ofEpochSecond(modifiedEpoch), ZoneId.systemDefault());

                String name = randomName(rnd, id);

                // CSV는 콤마/따옴표 안전하게 처리(이 예시는 name에 콤마를 넣지 않음)
                w.write(id + "," +
                        partnerId + "," +
                        name + "," +
                        status.name() + "," +
                        price + "," +
                        imageUrl + "," +
                        createdAt + "," +
                        modifiedAt);
                w.newLine();

                if ((i + 1) % 10_000 == 0) {
                    System.out.println("Progress: " + (i + 1) + "/" + totalProducts);
                }
            }
        }
    }

    static ProductStatus randomStatus(SplittableRandom rnd) {
        // 예시: ACTIVE 비중을 높이고 싶으면 가중치를 둘 수도 있음
        ProductStatus[] values = ProductStatus.values();
        return values[rnd.nextInt(values.length)];
    }

    static long randomPrice(SplittableRandom rnd, long min, long max) {
        return randomLong(rnd, min, max);
    }

    static long randomLong(SplittableRandom rnd, long minInclusive, long maxInclusive) {
        if (minInclusive > maxInclusive) throw new IllegalArgumentException("min > max");
        long bound = (maxInclusive - minInclusive) + 1;
        long r = rnd.nextLong(bound);
        return minInclusive + r;
    }

    static String randomName(SplittableRandom rnd, long id) {
        String[] adj = {"프리미엄", "스탠다드", "에센셜", "리미티드", "베이직", "프로", "라이트", "맥스"};
        String[] noun = {"키트", "세트", "패키지", "상품", "번들", "컬렉션", "옵션", "에디션"};
        return "상품-" + id + "-" + adj[rnd.nextInt(adj.length)] + "-" + noun[rnd.nextInt(noun.length)];
    }
}

