package arile.toy.stocksystem.bffserver.security.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    private static final long ACCESS_VALIDITY = 15 * 60 * 1000;
    private static final long REFRESH_VALIDITY = 7 * 24 * 60 * 60 * 1000;

    private final SecretKey key;

    public JwtService(@Value("${jwt.secret-key}") String key) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(key));
    }

    public String generateAccessToken(UserDetails userDetails) {

        var now = new Date();
        var exp = new Date(now.getTime() + ACCESS_VALIDITY);

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("type", "access")
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(UserDetails userDetails) {

        String jti = UUID.randomUUID().toString();
        var now = new Date();
        var exp = new Date(now.getTime() + REFRESH_VALIDITY);

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("type", "refresh")
                .claim("jti", jti)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    public String getUsernameFromAccessToken(String token) {
        try {
            Claims claims = parseClaims(token);

            if (!"access".equals(claims.get("type"))) {
                throw new JwtException("Invalid token type");
            }

            return claims.getSubject();

        } catch (ExpiredJwtException e) {
            return null;
        }
    }

    public String getUsernameFromRefreshToken(String token) {
        Claims claims = parseClaims(token);

        if (!"refresh".equals(claims.get("type"))) {
            throw new JwtException("Invalid token type");
        }

        return claims.getSubject();
    }

    public String getJtiFromRefreshToken(String token) {
        Claims claims = parseClaims(token);

        return claims.get("jti", String.class);
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            logger.warn("JWT expired at {}. Current time: {}. Allowed clock skew: {}ms",
                    e.getClaims().getExpiration(),
                    new Date(),
                    0);
            throw e;
        } catch (JwtException e) {
            logger.error("JWT parsing error", e);
            throw e;
        }
    }

    public long getRefreshValidity() {
        return REFRESH_VALIDITY;
    }
}
