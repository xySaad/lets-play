package com.z01.letsplay.model.DTO;

import com.z01.letsplay.model.Product;

public record ProductResponse(String id, String name, String description, Double price, String userId) {
    public static ProductResponse of(Product product) {
        return new ProductResponse(product.id, product.name, product.description, product.price, product.userId);
    }
}
