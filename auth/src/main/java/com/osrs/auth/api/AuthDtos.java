package com.osrs.auth.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotBlank @Size(min = 8, max = 128) String passwordConfirm,
        @NotBlank @Size(min = 1, max = 12) String characterName
    ) {
    }

    public record LoginRequest(
        @Email @NotBlank String email,
        @NotBlank String password
    ) {
    }

    public record RefreshRequest(
        @NotBlank String refreshToken
    ) {
    }

    public record LogoutRequest(
        @NotBlank String refreshToken
    ) {
    }

    public record AuthResponse(
        int accountId,
        int characterId,
        String characterName,
        String accessToken,
        String refreshToken,
        long accessTokenExpiresInSeconds,
        long refreshTokenExpiresInSeconds,
        boolean emailVerified
    ) {
    }

    public record MessageResponse(
        String message
    ) {
    }
}
