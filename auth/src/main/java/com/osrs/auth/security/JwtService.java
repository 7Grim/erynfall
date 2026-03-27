package com.osrs.auth.security;

import com.osrs.auth.config.AuthSettings;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtService {

    private final AuthSettings settings;
    private final SecretKey signingKey;

    public JwtService(AuthSettings settings) {
        this.settings = settings;
        this.signingKey = Keys.hmacShaKeyFor(settings.jwtSigningKey().getBytes(StandardCharsets.UTF_8));
    }

    public String issueAccessToken(int accountId, int characterId, String email) {
        Instant now = Instant.now();
        Instant exp = now.plus(settings.accessTokenTtl());

        return Jwts.builder()
            .issuer(settings.jwtIssuer())
            .audience().add(settings.jwtAudience()).and()
            .subject(Integer.toString(accountId))
            .claim("cid", characterId)
            .claim("email", email)
            .issuedAt(Date.from(now))
            .expiration(Date.from(exp))
            .signWith(signingKey)
            .compact();
    }
}
