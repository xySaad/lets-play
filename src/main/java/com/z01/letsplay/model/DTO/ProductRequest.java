package com.z01.letsplay.model.DTO;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ProductRequest {
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    public String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    public String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price must not be negative")
    @DecimalMax(value = "1000000000", message = "Price must not exceed 1,000,000,000")
    public Double price;
}
