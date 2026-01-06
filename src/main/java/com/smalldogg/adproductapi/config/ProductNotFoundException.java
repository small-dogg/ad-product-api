package com.smalldogg.adproductapi.config;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(Long productId) {
        super("Product not found. productId=" + productId);
    }
}