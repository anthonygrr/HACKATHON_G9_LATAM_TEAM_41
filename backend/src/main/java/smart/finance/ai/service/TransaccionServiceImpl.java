package smart.finance.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import smart.finance.ai.config.security.SecurityUtils;
import smart.finance.ai.dto.request.TransaccionRequestDTO;
import smart.finance.ai.dto.response.TransaccionResponseDTO;
import smart.finance.ai.entity.TipoTransaccion;
import smart.finance.ai.entity.Transaccion;
import smart.finance.ai.entity.Usuario;
import smart.finance.ai.exception.ForbiddenException;
import smart.finance.ai.exception.ResourceNotFoundException;
import smart.finance.ai.repository.TipoTransaccionRepository;
import smart.finance.ai.repository.TransaccionRepository;
import smart.finance.ai.repository.UsuarioRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransaccionServiceImpl implements TransaccionService {

    private final TransaccionRepository transaccionRepository;
    private final UsuarioRepository usuarioRepository;
    private final TipoTransaccionRepository tipoTransaccionRepository;
    private final ClasificacionService clasificacionService;

    @Override
    @Transactional
    public List<TransaccionResponseDTO> crearTransacciones(List<TransaccionRequestDTO> dtos) {
        dtos.forEach(dto -> SecurityUtils.assertOwnerOrAdmin(dto.getUsuarioId()));

        List<Transaccion> guardadas = transaccionRepository.saveAll(
                dtos.stream().map(this::buildTransaccion).collect(Collectors.toList()));
        return guardadas.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransaccionResponseDTO> listarPorUsuario(Integer usuarioId) {
        Integer effectiveUsuarioId = SecurityUtils.effectiveUserId(usuarioId);
        if (!usuarioRepository.existsById(effectiveUsuarioId)) {
            throw new ResourceNotFoundException("El usuario especificado no existe.");
        }
        return transaccionRepository.findByUsuarioId(effectiveUsuarioId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TransaccionResponseDTO obtenerPorId(Integer id) {
        Transaccion transaccion = obtenerEntidad(id);
        return mapToDTO(transaccion);
    }

    @Override
    @Transactional
    public TransaccionResponseDTO actualizarTransaccion(Integer id, TransaccionRequestDTO dto) {
        Transaccion transaccion = obtenerEntidad(id);

        if (dto.getUsuarioId() != null) {
            SecurityUtils.assertOwnerOrAdmin(dto.getUsuarioId());
            Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                    .orElseThrow(() -> new ResourceNotFoundException("El usuario especificado no existe."));
            transaccion.setUsuario(usuario);
        }

        transaccion.setDescripcion(dto.getDescripcion());
        transaccion.setMonto(dto.getMonto());
        transaccion.setTipoTransaccion(resolverTipoTransaccion(dto));

        Transaccion actualizada = transaccionRepository.save(transaccion);
        return mapToDTO(actualizada);
    }

    @Override
    @Transactional
    public void eliminarTransaccion(Integer id) {
        transaccionRepository.delete(obtenerEntidad(id));
    }

    private Transaccion obtenerEntidad(Integer id) {
        if (SecurityUtils.isAdmin()) {
            return transaccionRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("La transacción no existe."));
        }
        return transaccionRepository.findByIdAndUsuario_Id(id, SecurityUtils.currentUserId())
                .orElseGet(() -> {
                    if (transaccionRepository.existsById(id)) {
                        throw new ForbiddenException("La transacción no te pertenece.");
                    }
                    throw new ResourceNotFoundException("La transacción no existe.");
                });
    }

    private Transaccion buildTransaccion(TransaccionRequestDTO dto) {
        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("El usuario especificado no existe."));

        return Transaccion.builder()
                .usuario(usuario)
                .tipoTransaccion(resolverTipoTransaccion(dto))
                .descripcion(dto.getDescripcion())
                .monto(dto.getMonto())
                .fecha(LocalDate.now())
                .build();
    }

    private TipoTransaccion resolverTipoTransaccion(TransaccionRequestDTO dto) {
        Integer tipoId = (dto.getTipoTransaccionId() != null)
                ? dto.getTipoTransaccionId()
                : clasificacionService.predecirTipoTransaccion(dto.getDescripcion());

        return tipoTransaccionRepository.findById(tipoId)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de transacción inválido."));
    }

    private TransaccionResponseDTO mapToDTO(Transaccion t) {
        TransaccionResponseDTO clasificacion =
                clasificacionService.clasificarConProbabilidad(t.getDescripcion());
        Usuario usuario = t.getUsuario();

        return TransaccionResponseDTO.builder()
                .id(t.getId())
                .descripcion(t.getDescripcion())
                .categoria(clasificacion.getCategoria())
                .idCategoria(clasificacion.getIdCategoria())
                .monto(t.getMonto())
                .fecha(t.getFecha())
                .tipoTransaccion(t.getTipoTransaccion().getNombre())
                .probabilidad(clasificacion.getProbabilidad())
                .usuarioId(usuario.getId())
                .nombre(usuario.getNombre())
                .apellidoPaterno(usuario.getApellidoPaterno())
                .correo(usuario.getCorreo())
                .build();
    }

}