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

    public RefreshToken validateActiveToken(String token) {
        RefreshToken rt = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (rt.isRevoked()) {
            throw new IllegalArgumentException("Refresh token has been revoked");
        }

        if (rt.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Refresh token has expired");
        }

        return rt;
    }

    public void revoke(RefreshToken rt) {
        if (!rt.isRevoked()) {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        }
    }

    /**
     * Rotation (recommended): revoke old token and issue a new refresh token.
     * If you don't want rotation, you can skip this and reuse same refresh token.
     */
    public RefreshToken rotate(RefreshToken oldToken) {
        revoke(oldToken);
        return createRefreshToken(oldToken.getUser());
    }
}