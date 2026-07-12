package com.mawa3id.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${mawa3id.jwt.secret:}") String secret,
                      @Value("${mawa3id.jwt.expiration-ms}") long expirationMs) {
        if (secret == null || secret.isBlank()) {
            // No secret configured (e.g. local/dev/test). Generate an ephemeral key so
            // the app still runs, but tokens are invalidated on restart. Production MUST
            // set JWT_SECRET (a Base64-encoded key of at least 256 bits).
            this.key = Jwts.SIG.HS384.key().build();
            log.warn("No JWT secret configured (mawa3id.jwt.secret / JWT_SECRET is blank). "
                    + "Generated an ephemeral signing key; issued tokens will not survive a "
                    + "restart. Set JWT_SECRET for any non-local deployment.");
        } else {
            this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        }
        this.expirationMs = expirationMs;
    }

    public String generateToken(String subject, Map<String, Object> claims) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(subject)
                .claims(claims)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String extractSubject(String token) {
        return parse(token).getSubject();
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
