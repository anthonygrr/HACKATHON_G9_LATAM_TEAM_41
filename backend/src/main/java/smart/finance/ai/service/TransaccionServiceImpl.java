package smart.finance.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import smart.finance.ai.config.security.SecurityUtils;
import smart.finance.ai.dto.common.PageResponseDTO;
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
import smart.finance.ai.specification.TransaccionSpecification;
import smart.finance.ai.util.PaginacionUtils;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransaccionServiceImpl implements TransaccionService {

    private static final Set<String> CAMPOS_ORDEN = Set.of("id", "descripcion", "monto", "fecha");

    private final TransaccionRepository transaccionRepository;
    private final UsuarioRepository usuarioRepository;
    private final TipoTransaccionRepository tipoTransaccionRepository;
    private final ClasificacionService clasificacionService;

    @Override
    @Transactional
    public List<TransaccionResponseDTO> crearTransacciones(List<TransaccionRequestDTO> dtos) {
        dtos.forEach(dto -> dto.setUsuarioId(SecurityUtils.resolveOwnerOrAdmin(dto.getUsuarioId())));

        List<Transaccion> guardadas = transaccionRepository.saveAll(
                dtos.stream().map(this::buildTransaccion).collect(Collectors.toList()));
        return guardadas.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<TransaccionResponseDTO> listar(Integer usuarioId, String descripcion, String tipo,
                                                          LocalDate fechaInicio, LocalDate fechaFin,
                                                          int page, int size, String sort) {
        Integer effectiveUsuarioId;
        if (usuarioId == null) {
            effectiveUsuarioId = SecurityUtils.isAdmin() ? null : SecurityUtils.currentUserId();
        } else {
            effectiveUsuarioId = SecurityUtils.effectiveUserId(usuarioId);
        }
        if (effectiveUsuarioId != null && !usuarioRepository.existsById(effectiveUsuarioId)) {
            throw new ResourceNotFoundException("El usuario especificado no existe.");
        }

        Specification<Transaccion> spec = TransaccionSpecification.conFiltros(
                effectiveUsuarioId, descripcion, tipo, fechaInicio, fechaFin);
        Pageable pageable = PaginacionUtils.crearPageable(page, size, sort, CAMPOS_ORDEN, "id");

        Page<Transaccion> pagina = transaccionRepository.findAll(spec, pageable);
        return new PageResponseDTO<>(pagina.map(this::mapToDTO));
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

        dto.setUsuarioId(SecurityUtils.resolveOwnerOrAdmin(dto.getUsuarioId()));
        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("El usuario especificado no existe."));
        transaccion.setUsuario(usuario);

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

        return TransaccionResponseDTO.builder()
                .id(t.getId())
                .descripcion(t.getDescripcion())
                .categoria(clasificacion.getCategoria())
                .idCategoria(clasificacion.getIdCategoria())
                .monto(t.getMonto())
                .fecha(t.getFecha())
                .tipoTransaccion(t.getTipoTransaccion().getNombre())
                .probabilidad(clasificacion.getProbabilidad())
                .usuarioId(t.getUsuario().getId())
                .build();
    }

}