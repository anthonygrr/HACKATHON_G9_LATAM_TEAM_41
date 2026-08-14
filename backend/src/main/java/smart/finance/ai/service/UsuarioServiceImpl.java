package smart.finance.ai.service;





import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import smart.finance.ai.config.security.JwtProvider;
import smart.finance.ai.dto.request.UsuarioUpdateRequest;
import smart.finance.ai.dto.response.UsuarioResponse;
import smart.finance.ai.entity.Rol;
import smart.finance.ai.entity.Usuario;
import smart.finance.ai.exception.DuplicateEmailException;
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
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorId(Integer id) {
        return toResponse(buscarPorId(id));
    }

    @Override
    public UsuarioResponse actualizar(Integer id, UsuarioUpdateRequest request) {
        Usuario usuario = buscarPorId(id);

        userRepository.findByCorreo(request.correo()).ifPresent(usuarioConCorreo -> {
            if (!usuarioConCorreo.getId().equals(id)) {
                throw new DuplicateEmailException("El correo ya esta en uso");
            }
        });

        Rol rol = rolRepository.findById(request.rolId())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado: " + request.rolId()));

        usuario.setNombre(request.nombre());
        usuario.setApellidoPaterno(request.apellidoPaterno());
        usuario.setApellidoMaterno(request.apellidoMaterno());
        usuario.setFechaNacimiento(request.fechaNacimiento());
        usuario.setCorreo(request.correo());
        usuario.setRol(rol);

        return toResponse(userRepository.save(usuario));
    }

    @Override
    public void eliminar(Integer id) {
        Usuario usuario = buscarPorId(id);
        userRepository.delete(usuario);
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

