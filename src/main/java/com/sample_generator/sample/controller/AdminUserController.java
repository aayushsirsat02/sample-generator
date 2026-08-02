package com.sample_generator.sample.controller;

import com.sample_generator.sample.Entity.Role;
import com.sample_generator.sample.Entity.User;
import com.sample_generator.sample.repository.RoleRepository;
import com.sample_generator.sample.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserController(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> response = users.stream().map(user -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("username", user.getUsername());
            map.put("enabled", user.getEnabled());
            map.put("createdAt", user.getCreatedAt());
            map.put("roles", user.getRoles().stream().map(Role::getName).collect(Collectors.toList()));
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        String roleName = request.get("roleName");

        if (username == null || username.isBlank() || password == null || password.isBlank() || roleName == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid input data"));
        }

        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username already exists"));
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEnabled(true);

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
        user.setRoles(new HashSet<>(Set.of(role)));

        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "User created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));

        String username = (String) request.get("username");
        String password = (String) request.get("password");
        String roleName = (String) request.get("roleName");
        Boolean enabled = (Boolean) request.get("enabled");

        if (username != null && !username.isBlank()) {
            if (!username.equals(user.getUsername()) && userRepository.existsByUsername(username)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Username already exists"));
            }
            user.setUsername(username);
        }

        if (password != null && !password.isBlank()) {
            user.setPassword(passwordEncoder.encode(password));
        }

        if (enabled != null) {
            user.setEnabled(enabled);
        }

        if (roleName != null && !roleName.isBlank()) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
            user.setRoles(new HashSet<>(Set.of(role)));
        }

        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "User updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));

        if ("admin@spherical.com".equals(user.getUsername()) || "admin@aayush.com".equals(user.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cannot delete main administrator account"));
        }

        userRepository.delete(user);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }
}
