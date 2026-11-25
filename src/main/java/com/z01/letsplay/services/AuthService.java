package com.z01.letsplay.services;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.z01.letsplay.exception.AppError;
import com.z01.letsplay.model.Session;
import com.z01.letsplay.model.User;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

@Service
public class AuthService {
    private static final int SESSION_DAYS = 3;
    private static final int BCRYPT_MAX_BYTES = 72;

    @Autowired
    private SecretKey jwtKey;
    @Autowired
    private Session.repo sessionRepo;
    @Autowired
    private User.repo userRepo;

    // BCrypt generates a random salt per hash and stores it inside the hash
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public ResponseCookie startSession(User user) {
        String jwt = Jwts.builder()
                .setSubject(user.id)
                .setIssuedAt(new Date())
                .signWith(jwtKey)
                .compact();

        Session session = new Session();
        session.userId = user.id;
        session.jwt = jwt;
        session.createdAt = LocalDateTime.now();
        sessionRepo.save(session);

        return ResponseCookie.from("jwt", jwt)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(60 * 60 * 24 * SESSION_DAYS)
                .sameSite("Lax")
                .build();
    }

    public String getUserId(String jwt) {
        return getUser(jwt).id;
    }

    /**
     * Validates the token (signature, matching live session, age) and loads its
     * user.
     * The user, and so the role, is read from the database on every call: deleting
     * a
     * user or changing a role applies immediately.
     */
    public User getUser(String jwt) {
        String userId;
        try {
            userId = Jwts.parserBuilder()
                    .setSigningKey(jwtKey)
                    .build()
                    .parseClaimsJws(jwt)
                    .getBody()
                    .getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            throw AppError.INVALID_JWT.asException();
        }

        Session session = sessionRepo.findById(userId).orElseThrow(AppError.INVALID_JWT::asException);
        if (!session.jwt.equals(jwt) || session.createdAt.plusDays(SESSION_DAYS).isBefore(LocalDateTime.now()))
            throw AppError.INVALID_JWT.asException();

        return userRepo.findById(userId).orElseThrow(AppError.INVALID_JWT::asException);
    }

    public User login(String email, String rawPassword) {
        User user = userRepo.findByEmail(User.normalizeEmail(email))
                .orElseThrow(AppError.INVALID_EMAIL_OR_PASSWORD::asException);
        if (!matches(rawPassword, user.password))
            throw AppError.INVALID_EMAIL_OR_PASSWORD.asException();
        return user;
    }

    public String hash(String rawPassword) {
        if (exceedsBcryptLimit(rawPassword))
            throw AppError.PASSWORD_TOO_LONG.asException();
        return encoder.encode(rawPassword);
    }

    private boolean matches(String rawPassword, String hash) {
        return !exceedsBcryptLimit(rawPassword) && encoder.matches(rawPassword, hash);
    }

    // Spring Security's BCrypt throws IllegalArgumentException above 72 bytes;
    // refuse
    // such input up front instead of letting it surface as a 500.
    private static boolean exceedsBcryptLimit(String rawPassword) {
        return rawPassword.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES;
    }
}
