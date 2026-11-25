package com.z01.letsplay.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import com.z01.letsplay.model.Role;
import com.z01.letsplay.model.User;
import com.z01.letsplay.services.AuthService;

@Component
public class RootBootstrap implements ApplicationListener<ContextRefreshedEvent> {
    @Autowired
    private User.repo userRepo;
    @Autowired
    private AuthService authService;

    @Value("${ROOT_PASSWORD:}")
    private String rootPasswordEnv;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        User root = userRepo.findByEmail("root").orElseGet(() -> {
            char[] password = resolvePassword();
            User u = new User();
            u.name = "root";
            u.email = "root";
            u.password = authService.hash(new String(password));
            u.role = Role.ROOT;
            return userRepo.save(u);
        });

        if (!Role.ROOT.equals(root.role)) {
            root.role = Role.ROOT;
            userRepo.save(root);
        }
    }

    private char[] resolvePassword() {
        if (!rootPasswordEnv.isBlank()) {
            return rootPasswordEnv.toCharArray();
        }

        if (System.console() != null) {
            System.out.print("Enter root password: ");
            char[] pw = System.console().readPassword();
            System.out.print("Confirm root password: ");
            char[] confirm = System.console().readPassword();

            if (!Arrays.equals(pw, confirm))
                throw new IllegalStateException("Passwords do not match");

            return pw;
        }

        throw new IllegalStateException(
                "Root password not set. Provide ROOT_PASSWORD env var or run interactively.");
    }
}
