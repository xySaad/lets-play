package com.z01.letsplay.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.repository.MongoRepository;

// one session per user: logging in again replaces the previous token
@Document(collection = "sessions")
public class Session {
    @Id
    public String userId;

    @Field("jwt")
    public String jwt;

    @Field("createdAt")
    public LocalDateTime createdAt;

    public interface repo extends MongoRepository<Session, String> {
    }
}
