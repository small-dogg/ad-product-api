package com.smalldogg.adproductapi.product.response;

public record BulkAddResponse(long totalLines, long insertedOrUpdatedRows, long skippedRows) {
}
