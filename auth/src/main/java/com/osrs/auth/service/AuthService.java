package com.osrs.auth.service;

import com.osrs.auth.api.AuthDtos;
import com.osrs.auth.config.AuthSettings;
import com.osrs.auth.model.AccountRecord;
import com.osrs.auth.model.CharacterRecord;
import com.osrs.auth.repo.AuthRepository;
import com.osrs.auth.security.JwtService;
import com.osrs.auth.security.PasswordHasher;
import com.osrs.auth.security.TokenUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuthService {

    private final AuthRepository authRepository;
    private final PasswordHasher passwordHasher;
    private final TokenUtils tokenUtils;
    private final JwtService jwtService;
    private final AuthSettings settings;

    public AuthService(AuthRepository authRepository,
                       PasswordHasher passwordHasher,
                       TokenUtils tokenUtils,
                       JwtService jwtService,
                       AuthSettings settings) {
        this.authRepository = authRepository;
        this.passwordHasher = passwordHasher;
        this.tokenUtils = tokenUtils;
        this.jwtService = jwtService;
        this.settings = settings;
    }

    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) throws Exception {
        String email = request.email().trim().toLowerCase();
        String characterName = sanitizeCharacterName(request.characterName());

        if (!request.password().equals(request.passwordConfirm())) {
            throw new AuthValidationException("Passwords do not match.");
        }
        if (authRepository.findAccountByEmail(email) != null) {
            throw new AuthValidationException("An account with that email already exists.");
        }

        String hash = passwordHasher.hash(request.password());
        boolean emailVerified = settings.bypassEmailVerification();
        AuthRepository.RegistrationRecord record = authRepository.createAccountAndCharacter(
            email,
            hash,
            emailVerified,
            characterName
        );

        return issueTokens(record.account(), record.character());
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) throws Exception {
        String email = request.email().trim().toLowerCase();
        AccountRecord account = authRepository.findAccountByEmail(email);
        if (account == null || !passwordHasher.verify(request.password(), account.passwordHash())) {
            throw new AuthValidationException("Invalid email or password.");
        }
        if (!account.emailVerified() && !settings.bypassEmailVerification()) {
            throw new AuthValidationException("Please verify your email before logging in.");
        }
        if (account.status() != 1) {
            throw new AuthValidationException("Account is not active.");
        }

        CharacterRecord character = authRepository.getActiveCharacter(account.id());
        if (character == null) {
            throw new AuthValidationException("No active character found for this account.");
        }

        authRepository.touchLastLogin(account.id());
        return issueTokens(account, character);
    }

    public AuthDtos.AuthResponse refresh(AuthDtos.RefreshRequest request) throws Exception {
        String tokenHash = tokenUtils.sha256(request.refreshToken().trim());
        Integer accountId = authRepository.findRefreshTokenAccount(tokenHash);
        if (accountId == null) {
            throw new AuthValidationException("Invalid refresh token.");
        }

        AccountRecord account = authRepository.findAccountById(accountId);
        if (account == null || account.status() != 1) {
            throw new AuthValidationException("Account is not active.");
        }
        CharacterRecord character = authRepository.getActiveCharacter(account.id());
        if (character == null) {
            throw new AuthValidationException("No active character found for this account.");
        }

        authRepository.revokeRefreshToken(tokenHash);
        return issueTokens(account, character);
    }

    public void logout(AuthDtos.LogoutRequest request) throws Exception {
        String tokenHash = tokenUtils.sha256(request.refreshToken().trim());
        authRepository.revokeRefreshToken(tokenHash);
    }

    private AuthDtos.AuthResponse issueTokens(AccountRecord account, CharacterRecord character) throws Exception {
        String accessToken = jwtService.issueAccessToken(account.id(), character.id(), account.email());
        String refreshToken = tokenUtils.randomToken();
        String refreshHash = tokenUtils.sha256(refreshToken);
        authRepository.storeRefreshToken(account.id(), refreshHash, Instant.now().plus(settings.refreshTokenTtl()));

        return new AuthDtos.AuthResponse(
            account.id(),
            character.id(),
            character.characterName(),
            accessToken,
            refreshToken,
            settings.accessTokenTtl().toSeconds(),
            settings.refreshTokenTtl().toSeconds(),
            account.emailVerified() || settings.bypassEmailVerification()
        );
    }

    private String sanitizeCharacterName(String raw) {
        String name = raw.trim();
        if (!name.matches("[A-Za-z0-9 ]{1,12}")) {
            throw new AuthValidationException("Character name must be 1-12 characters (letters, numbers, spaces).");
        }
        return name;
    }

    public static class AuthValidationException extends RuntimeException {
        public AuthValidationException(String message) {
            super(message);
        }
    }
}
