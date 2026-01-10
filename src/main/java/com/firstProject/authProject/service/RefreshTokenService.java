package com.firstProject.authProject.service;

import com.firstProject.authProject.config.JwtProperties;
import com.firstProject.authProject.entity.RefreshToken;
import com.firstProject.authProject.entity.User;
import com.firstProject.authProject.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties props;

    public RefreshToken createRefreshToken(User user) {
        RefreshToken rt = RefreshToken.builder()
                .token(UUID.randomUUID().toString()) // random opaque token
                .user(user)
                .expiresAt(Instant.now().plus(props.getRefreshTokenDays(), ChronoUnit.DAYS))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(rt);
    }
}