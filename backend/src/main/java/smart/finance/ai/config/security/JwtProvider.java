package smart.finance.ai.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Service
public class JwtProvider {

    private final SecretKey key;

    public JwtProvider(SecretKey jwtSecretKey) {
        this.key = jwtSecretKey;
    }

    public String generateToken(Authentication authentication){

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        String roles = populateAuthorities(authorities);

        String jwt = Jwts.builder().issuedAt(new Date())
                .expiration(new Date(new Date().getTime() + 86400000))
                .claim("email", authentication.getName())
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

