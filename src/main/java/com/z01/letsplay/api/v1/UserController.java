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

import com.z01.letsplay.annotation.EntityAccess;
import com.z01.letsplay.annotation.EntityAccess.Mode;
import com.z01.letsplay.annotation.RequiresPermission;
import com.z01.letsplay.model.User;
import com.z01.letsplay.model.DTO.UserCreateRequest;
import com.z01.letsplay.model.DTO.UserResponse;
import com.z01.letsplay.model.DTO.UserUpdateRequest;
import com.z01.letsplay.services.UserService;

import jakarta.validation.Valid;

@RestController
public class UserController {
    @Autowired
    private User.repo userRepo;
    @Autowired
    private UserService userService;

    @GetMapping("/api/v1/users")
    @RequiresPermission(scope = "v1:users:list", description = "list users")
    public List<UserResponse> getAll() {
        return userRepo.findAll().stream().map(UserResponse::of).toList();
    }

    @GetMapping("/api/v1/users/{user}")
    @RequiresPermission(scope = "v1:users:read", description = "view a user")
    public UserResponse getById(@EntityAccess(mode = Mode.Read) User user) {
        return UserResponse.of(user);
    }

    @PostMapping("/api/v1/users")
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission(scope = "v1:users:create", description = "create a user")
    public UserResponse create(@RequestBody @Valid UserCreateRequest body) {
        return UserResponse.of(userService.create(body.name, body.email, body.password, body.role));
    }

    @PutMapping("/api/v1/users/{user}")
    @RequiresPermission(scope = "v1:users:update", description = "update a user")
    public UserResponse update(@EntityAccess(mode = Mode.Write) User user, @RequestBody @Valid UserUpdateRequest body) {
        return UserResponse.of(userService.update(user, body));
    }

    @DeleteMapping("/api/v1/users/{user}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission(scope = "v1:users:delete", description = "delete a user and their products")
    public void delete(@EntityAccess(mode = Mode.Write) User user) {
        userService.delete(user);
    }
}
