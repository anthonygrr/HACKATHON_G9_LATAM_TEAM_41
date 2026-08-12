package smart.finance.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import smart.finance.ai.config.security.JwtProvider;
import smart.finance.ai.dto.request.LoginRequest;
import smart.finance.ai.dto.request.SignupRequest;
import smart.finance.ai.dto.response.AuthResponse;
import smart.finance.ai.entity.Rol;
import smart.finance.ai.entity.Usuario;
import smart.finance.ai.exception.DuplicateEmailException;
import smart.finance.ai.repository.RolRepository;
import smart.finance.ai.repository.UsuarioRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Integer DEFAULT_ROLE_ID = 2;

    private final UsuarioRepository userRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.findByCorreo(request.correo()).isPresent()) {
            throw new DuplicateEmailException("El correo ya esta en uso");
        }

        Rol defaultRole = rolRepository.findById(DEFAULT_ROLE_ID)
                .orElseThrow(() -> new RuntimeException("No se encontro el Rol"));

        Usuario user = Usuario.builder()
                .nombre(request.nombre())
                .apellidoPaterno(request.apellidoPaterno())
                .apellidoMaterno(request.apellidoMaterno())
                .fechaNacimiento(request.fechaNacimiento())
                .correo(request.correo())
                .contrasena(passwordEncoder.encode(request.contrasena()))
                .rol(defaultRole)
                .build();

        Usuario savedUser = userRepository.save(user);

        String jwt = buildJwt(savedUser);

        return AuthResponse.builder()
                .jwt(jwt)
                .message("Registro exitoso")
                .rol(savedUser.getRol().getNombre())
                .build();
    }

    @Override
    public AuthResponse signin(LoginRequest request) {
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(request.correo());

        if (!passwordEncoder.matches(request.contrasena(), userDetails.getPassword())) {
            throw new BadCredentialsException("Usuario o contraseña incorrectos");
        }

        Integer userId = userRepository.findByCorreo(request.correo())
                .map(Usuario::getId)
                .orElseThrow(() -> new BadCredentialsException("Usuario o contraseña incorrectos"));

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        String jwt = jwtProvider.generateToken(authentication, userId);
        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse(null);

        return AuthResponse.builder()
                .jwt(jwt)
                .message("Inicio de sesion exitoso")
                .rol(role)
                .build();
    }

    private String buildJwt(Usuario usuario) {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(usuario.getRol().getNombre()));
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                usuario.getCorreo(), null, authorities);
        return jwtProvider.generateToken(authentication, usuario.getId());
    }
}