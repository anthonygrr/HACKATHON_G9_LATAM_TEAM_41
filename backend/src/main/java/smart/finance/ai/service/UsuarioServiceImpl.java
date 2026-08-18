package smart.finance.ai.service;





import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import smart.finance.ai.config.security.JwtProvider;
import smart.finance.ai.config.security.SecurityUtils;
import smart.finance.ai.dto.request.UsuarioCreateRequest;
import smart.finance.ai.dto.request.UsuarioUpdateRequest;
import smart.finance.ai.dto.response.UsuarioResponse;
import smart.finance.ai.entity.Rol;
import smart.finance.ai.entity.Usuario;
import smart.finance.ai.exception.DuplicateEmailException;
import smart.finance.ai.exception.ForbiddenException;
import smart.finance.ai.exception.ResourceNotFoundException;
import smart.finance.ai.repository.RolRepository;
import smart.finance.ai.repository.UsuarioRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository userRepository;
    private final RolRepository rolRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Usuario findUserByJwtToken(String jwt) {
        String email = jwtProvider.getEmailFromJwtToken(jwt);
        return userRepository.findByCorreo(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));
    }

    @Override
    public Usuario findUserByEmail(String email) {
        return userRepository.findByCorreo(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));
    }

    @Override
    public UsuarioResponse crear(UsuarioCreateRequest request) {
        if (!SecurityUtils.isAdmin()) {
            throw new ForbiddenException("Solo los administradores pueden crear usuarios");
        }
        if (userRepository.findByCorreo(request.correo()).isPresent()) {
            throw new DuplicateEmailException("El correo ya esta en uso");
        }

        Rol rol = rolRepository.findById(request.rolId())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado: " + request.rolId()));

        Usuario usuario = Usuario.builder()
                .nombre(request.nombre())
                .apellidoPaterno(request.apellidoPaterno())
                .apellidoMaterno(request.apellidoMaterno())
                .fechaNacimiento(request.fechaNacimiento())
                .correo(request.correo())
                .contrasena(passwordEncoder.encode(request.contrasena()))
                .rol(rol)
                .build();

        return toResponse(userRepository.save(usuario));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        if (!SecurityUtils.isAdmin()) {
            throw new ForbiddenException("Solo los administradores pueden listar usuarios");
        }
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorId(Integer id) {
        return toResponse(buscarPorId(SecurityUtils.effectiveUserId(id)));
    }

    @Override
    public UsuarioResponse actualizar(Integer id, UsuarioUpdateRequest request) {
        Integer targetId = SecurityUtils.effectiveUserId(id);
        Usuario usuario = buscarPorId(targetId);

        userRepository.findByCorreo(request.correo()).ifPresent(usuarioConCorreo -> {
            if (!usuarioConCorreo.getId().equals(targetId)) {
                throw new DuplicateEmailException("El correo ya esta en uso");
            }
        });

        if (SecurityUtils.isAdmin()) {
            aplicarRol(usuario, request.rolId());
        }

        usuario.setNombre(request.nombre());
        usuario.setApellidoPaterno(request.apellidoPaterno());
        usuario.setApellidoMaterno(request.apellidoMaterno());
        usuario.setFechaNacimiento(request.fechaNacimiento());
        usuario.setCorreo(request.correo());

        return toResponse(userRepository.save(usuario));
    }

    @Override
    public void eliminar(Integer id) {
        Usuario usuario = buscarPorId(SecurityUtils.effectiveUserId(id));
        userRepository.delete(usuario);
    }

    private void aplicarRol(Usuario usuario, Integer rolId) {
        if (rolId == null) {
            return;
        }
        Rol rol = rolRepository.findById(rolId)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado: " + rolId));
        usuario.setRol(rol);
    }

    private Usuario buscarPorId(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));
    }

    private UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellidoPaterno(),
                usuario.getApellidoMaterno(),
                usuario.getFechaNacimiento(),
                usuario.getCorreo(),
                usuario.getRol().getNombre()
        );
    }
}

