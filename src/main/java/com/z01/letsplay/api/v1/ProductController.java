package com.z01.letsplay.api.v1;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.z01.letsplay.annotation.Auth;
import com.z01.letsplay.annotation.EntityAccess;
import com.z01.letsplay.annotation.EntityAccess.Mode;
import com.z01.letsplay.model.Product;
import com.z01.letsplay.model.DTO.ProductRequest;
import com.z01.letsplay.model.DTO.ProductResponse;

import jakarta.validation.Valid;

@RestController
public class ProductController {
    @Autowired
    private Product.repo productRepo;

    @GetMapping("/api/v1/products")
    public List<ProductResponse> getAll() {
        return productRepo.findAll().stream().map(ProductResponse::of).toList();
    }

    @GetMapping("/api/v1/products/{product}")
    public ProductResponse getById(@EntityAccess(mode = Mode.Read) Product product) {
        return ProductResponse.of(product);
    }

    @PostMapping("/api/v1/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Auth.User String userId, @RequestBody @Valid ProductRequest body) {
        Product product = new Product();
        apply(product, body);
        product.userId = userId;
        return ProductResponse.of(productRepo.save(product));
    }

    @PutMapping("/api/v1/products/{product}")
    public ProductResponse update(@EntityAccess(mode = Mode.Write) Product product,
            @RequestBody @Valid ProductRequest body) {
        apply(product, body);
        return ProductResponse.of(productRepo.save(product));
    }

    @DeleteMapping("/api/v1/products/{product}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@EntityAccess(mode = Mode.Write) Product product) {
        productRepo.delete(product);
    }

    private static void apply(Product product, ProductRequest body) {
        product.name = body.name.trim();
        product.description = body.description == null ? null : body.description.trim();
        product.price = body.price;
    }
}
