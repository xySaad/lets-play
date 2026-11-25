package com.z01.letsplay.model;

import java.util.Optional;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.z01.letsplay.annotation.AccessMethod;
import com.z01.letsplay.annotation.EntityAccess.Mode;
import com.z01.letsplay.exception.AppError;
import com.z01.letsplay.infrastructure.RestrictedEntity;

@Document(collection = "products")
public class Product implements RestrictedEntity<User> {
    @Id
    public String id;

    @Field("name")
    public String name;

    @Field("description")
    public String description;

    @Field("price")
    public Double price;

    @Indexed
    @Field("userId")
    public String userId;

    @Override
    public void ensureAccess(User user, Mode mode) {
        if (mode == Mode.Read)
            return;

        if (user != null && (Role.ROOT.equals(user.role) || user.id.equals(userId)))
            return;

        throw AppError.ACCESS_DENIED.asException();
    }

    public interface repo extends MongoRepository<Product, String> {
        @AccessMethod
        @Override
        Optional<Product> findById(String id);

        void deleteByUserId(String userId);
    }
}
