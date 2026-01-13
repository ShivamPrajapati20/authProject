package com.firstProject.authProject.service;

import com.firstProject.authProject.dto.request.LoginRequest;
import com.firstProject.authProject.dto.request.LogoutRequest;
import com.firstProject.authProject.dto.request.RefreshRequest;
import com.firstProject.authProject.dto.request.RegisterRequest;
import com.firstProject.authProject.dto.response.AuthResponse;
import com.firstProject.authProject.dto.response.RegisterResponse;
import com.firstProject.authProject.entity.RefreshToken;
import com.firstProject.authProject.entity.Role;
import com.firstProject.authProject.entity.User;
import com.firstProject.authProject.repository.RoleRepository;
import com.firstProject.authProject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.firstProject.authProject.security.JwtService;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final com.firstProject.authProject.config.JwtProperties jwtProperties;

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

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!user.isEnabled()) {
            throw new IllegalArgumentException("User account is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        // Add useful claims (keep minimal)
        String role = user.getRoles().stream().findFirst().map(Role::getName).orElse("ROLE_USER");
        String accessToken = jwtService.generateAccessToken(
                user.getEmail(),
                Map.of("role", role)
        );

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return new AuthResponse(
                accessToken,
                refreshToken.getToken(),
                "Bearer",
                15L * 60L // matches your default access token minutes
        );
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken current = refreshTokenService.validateActiveToken(request.getRefreshToken());
        User user = current.getUser();

        String role = user.getRoles().stream().findFirst().map(Role::getName).orElse("ROLE_USER");

        String newAccessToken = jwtService.generateAccessToken(
                user.getEmail(),
                Map.of("role", role)
        );

        // rotate refresh token (recommended)
        RefreshToken newRefresh = refreshTokenService.rotate(current);

        long expiresInSeconds = jwtProperties.getAccessTokenMinutes() * 60;

        return new AuthResponse(
                newAccessToken,
                newRefresh.getToken(),
                "Bearer",
                expiresInSeconds
        );
    }

    public void logout(LogoutRequest request) {
        RefreshToken rt = refreshTokenService.validateActiveToken(request.getRefreshToken());
        refreshTokenService.revoke(rt);
    }
}
