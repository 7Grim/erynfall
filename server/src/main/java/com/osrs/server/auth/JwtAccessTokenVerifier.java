package com.osrs.server.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

public class JwtAccessTokenVerifier {

    private final AuthTokenSettings settings;
    private final SecretKey signingKey;

    public JwtAccessTokenVerifier(AuthTokenSettings settings) {
        this.settings = settings;
        String configuredKey = settings.jwtSigningKey();
        this.signingKey = configuredKey.isBlank()
            ? null
            : Keys.hmacShaKeyFor(configuredKey.getBytes(StandardCharsets.UTF_8));
    }

    public boolean isConfigured() {
        return signingKey != null;
    }

    public VerifiedAccessToken verify(String token) {
        if (signingKey == null) {
            throw new IllegalStateException("JWT_SIGNING_KEY is not configured");
        }

        Claims claims = Jwts.parser()
            .verifyWith(signingKey)
            .requireIssuer(settings.jwtIssuer())
            .build()
            .parseSignedClaims(token)
            .getPayload();

        if (!hasAudience(claims, settings.jwtAudience())) {
            throw new JwtException("Invalid token audience");
        }

        int accountId = parsePositiveInt(claims.getSubject(), "sub");
        int characterId = parseClaimInt(claims.get("cid"), "cid");
        String email = claims.get("email", String.class);

        return new VerifiedAccessToken(accountId, characterId, email == null ? "" : email);
    }

    private static boolean hasAudience(Claims claims, String expectedAudience) {
        Object aud = claims.get("aud");
        if (aud instanceof String audString) {
            return expectedAudience.equals(audString);
        }
        if (aud instanceof Collection<?> audValues) {
            return audValues.stream().anyMatch(value -> expectedAudience.equals(String.valueOf(value)));
        }
        return false;
    }

    private static int parseClaimInt(Object raw, String claimName) {
        if (raw instanceof Number number) {
            int value = number.intValue();
            if (value > 0) {
                return value;
            }
        }
        throw new JwtException("Invalid " + claimName + " claim");
    }

    private static int parsePositiveInt(String raw, String claimName) {
        if (raw == null || raw.isBlank()) {
            throw new JwtException("Invalid " + claimName + " claim");
        }
        try {
            int value = Integer.parseInt(raw);
            if (value <= 0) {
                throw new JwtException("Invalid " + claimName + " claim");
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new JwtException("Invalid " + claimName + " claim", ex);
        }
    }

    public record VerifiedAccessToken(int accountId, int characterId, String email) {
    }
}
