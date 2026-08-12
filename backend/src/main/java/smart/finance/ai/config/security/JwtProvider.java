package smart.finance.ai.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class JwtProvider {

    private final SecretKey key;
    private final String issuer;
    private final long expirationMs;

    public JwtProvider(SecretKey jwtSecretKey,
                       @Value("${app.jwt.issuer:finance-ai}") String issuer,
                       @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
        this.key = jwtSecretKey;
        this.issuer = issuer;
        this.expirationMs = expirationMs;
    }

    public String generateToken(Authentication authentication, Integer userId) {

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        String roles = populateAuthorities(authorities);

        String jwt = Jwts.builder()
                .issuer(issuer)
                .issuedAt(new Date())
                .expiration(new Date(new Date().getTime() + expirationMs))
                .id(UUID.randomUUID().toString())
                .claim("email", authentication.getName())
                .claim("uid", userId)
                .claim("authorities", roles)
                .signWith(key)
                .compact();

        return jwt;
    }

    public String getEmailFromJwtToken(String jwt){
        jwt = jwt.substring(7);

        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(jwt).getPayload();

        String email = String.valueOf(claims.get("email"));

        return email;
    }

    private String populateAuthorities(Collection<? extends GrantedAuthority> authorities) {
        Set<String> auths = new HashSet<>();

        for (GrantedAuthority authority: authorities){
            auths.add(authority.getAuthority());
        }

        return String.join(",", auths);
    }
}

