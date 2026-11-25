package com.z01.letsplay.model;

import java.util.Locale;
import java.util.Optional;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.z01.letsplay.annotation.AccessMethod;

@Document(collection = "users")
public class User {
    @Id
    public String id;

    @Field("name")
    public String name;

    @Indexed(unique = true)
    @Field("email")
    public String email;

    @Field("password")
    public String password;

    @Field("role")
    public String role;

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public interface repo extends MongoRepository<User, String> {
        @AccessMethod
        @Override
        Optional<User> findById(String id);

        Optional<User> findByEmail(String email);

        boolean existsByEmail(String email);
    }
}
