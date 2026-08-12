package smart.finance.ai.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.List;

@Service
public class JwtTokenValidator extends OncePerRequestFilter {

    private final SecretKey key;

    public JwtTokenValidator(SecretKey jwtSecretKey) {
        this.key = jwtSecretKey;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String jwt = request.getHeader(JwtConstant.JWT_HEADER);

        if (jwt != null){
            jwt = jwt.substring(7);
            try {

//                    Claims claims = Jwts.parser().setSigningKey(key).build().parseClaimsJws().getBody();

                Claims claims = Jwts.parser().verifyWith(key).build()
                        .parseSignedClaims(jwt).getPayload();

                String email = claims.get("email", String.class);
                Integer userId = claims.get("uid", Integer.class);

                String authorities = String.valueOf(claims.get("authorities")); // roles

                List<GrantedAuthority> authorityList = AuthorityUtils.commaSeparatedStringToAuthorityList(authorities);

                AuthenticatedUser principal = new AuthenticatedUser(userId, email, authorityList);

                Authentication authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal, null, authorityList);

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (Exception e){
                throw new BadCredentialsException("Invalid token");
            }
        }
        // continue with the next filter
        filterChain.doFilter(request, response);
    }

}

