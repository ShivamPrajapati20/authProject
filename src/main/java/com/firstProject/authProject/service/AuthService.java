package com.firstProject.authProject.service;

import com.firstProject.authProject.dto.request.RegisterRequest;
import com.firstProject.authProject.dto.response.RegisterResponse;
import com.firstProject.authProject.entity.Role;
import com.firstProject.authProject.entity.User;
import com.firstProject.authProject.repository.RoleRepository;
import com.firstProject.authProject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if(userRepository.existsByEmail(email)){
            throw new IllegalArgumentException("Email already exists");
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new IllegalArgumentException("Role is not found"));

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);
        user.setRoles(Set.of(userRole));
        User saved = userRepository.save(user);

        return new RegisterResponse(saved.getId(), saved.getEmail(), "User registered successfully");

    }
}
