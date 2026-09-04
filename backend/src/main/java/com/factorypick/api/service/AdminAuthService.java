package com.factorypick.api.service;

import com.factorypick.api.dto.LoginResponse;
import com.factorypick.api.exception.UnauthorizedException;
import com.factorypick.api.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminAuthService {
    private final AdminRepository admins;
    private final PasswordEncoder encoder;
    private final Duration tokenLifetime;
    private final Map<String, Instant> tokens = new ConcurrentHashMap<>();

    public AdminAuthService(AdminRepository admins, PasswordEncoder encoder,
                            @Value("${factorypick.admin.token-hours:8}") long tokenHours) {
        this.admins = admins; this.encoder = encoder; this.tokenLifetime = Duration.ofHours(tokenHours);
    }

    public LoginResponse login(String username, String password) {
        String hash = admins.findPasswordHash(username)
                .orElseThrow(() -> new UnauthorizedException("아이디 또는 비밀번호가 올바르지 않습니다."));
        if (!encoder.matches(password, hash)) throw new UnauthorizedException("아이디 또는 비밀번호가 올바르지 않습니다.");
        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(tokenLifetime);
        tokens.put(token, expiresAt);
        return new LoginResponse(token, expiresAt);
    }

    public void validate(String token) {
        if (token == null || token.isBlank()) throw new UnauthorizedException("관리자 인증이 필요합니다.");
        Instant expiry = tokens.get(token);
        if (expiry == null || expiry.isBefore(Instant.now())) {
            tokens.remove(token); throw new UnauthorizedException("관리자 인증이 만료되었거나 유효하지 않습니다.");
        }
    }
    public void logout(String token) { if (token != null) tokens.remove(token); }
}
