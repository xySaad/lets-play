package com.z01.letsplay.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.z01.letsplay.exception.AppError;
import com.z01.letsplay.model.Product;
import com.z01.letsplay.model.Session;
import com.z01.letsplay.model.User;
import com.z01.letsplay.model.DTO.UserUpdateRequest;

@Service
public class UserService {
    @Autowired
    private User.repo userRepo;
    @Autowired
    private Product.repo productRepo;
    @Autowired
    private Session.repo sessionRepo;
    @Autowired
    private AuthService authService;

    public List<User> getAll() {
        return userRepo.findAll();
    }

    public User create(String name, String email, String rawPassword, String role) {
        String normalizedEmail = User.normalizeEmail(email);
        if (userRepo.existsByEmail(normalizedEmail))
            throw AppError.EMAIL_ALREADY_EXISTS.asException();

        User user = new User();
        user.name = name.trim();
        user.email = normalizedEmail;
        user.password = authService.hash(rawPassword);
        user.role = role;
        return userRepo.save(user);
    }

    public User update(User user, UserUpdateRequest updateDetails) {
        String normalizedEmail = User.normalizeEmail(updateDetails.email);
        if (!normalizedEmail.equals(user.email) && userRepo.existsByEmail(normalizedEmail))
            throw AppError.EMAIL_ALREADY_EXISTS.asException();

        user.name = updateDetails.name.trim();
        user.email = normalizedEmail;
        user.role = updateDetails.role;
        if (updateDetails.password != null)
            user.password = authService.hash(updateDetails.password);
        return userRepo.save(user);
    }

    // a user owns products: remove them too instead of leaving orphans behind
    public void delete(User user) {
        productRepo.deleteByUserId(user.id);
        sessionRepo.deleteById(user.id);
        userRepo.delete(user);
    }
}
