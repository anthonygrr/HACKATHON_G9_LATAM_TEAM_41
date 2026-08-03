package smart.finance.ai.service;





import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import smart.finance.ai.config.security.JwtProvider;
import smart.finance.ai.entity.Usuario;
import smart.finance.ai.repository.UsuarioRepository;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository userRepository;
    private final JwtProvider jwtProvider;

    @Override
    public Usuario findUserByJwtToken(String jwt) {
        String email = jwtProvider.getEmailFromJwtToken(jwt);
        return userRepository.findByCorreo(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    @Override
    public Usuario findUserByEmail(String email) {
        return userRepository.findByCorreo(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }
}

