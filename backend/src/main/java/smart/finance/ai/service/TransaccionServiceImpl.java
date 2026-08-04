package smart.finance.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import smart.finance.ai.dto.request.TransaccionRequestDTO;
import smart.finance.ai.dto.response.TransaccionResponseDTO;
import smart.finance.ai.entity.TipoTransaccion;
import smart.finance.ai.entity.Transaccion;
import smart.finance.ai.entity.Usuario;
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
    private final ClasificadorService clasificadorService;

    @Override
    @Transactional
    public TransaccionResponseDTO crearTransaccion(TransaccionRequestDTO dto) {
        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("El usuario especificado no existe."));

        Integer tipoId = (dto.getTipoTransaccionId() != null)
                ? dto.getTipoTransaccionId()
                : clasificadorService.predecirTipoTransaccion(dto.getDescripcion());

        TipoTransaccion tipoTransaccion = tipoTransaccionRepository.findById(tipoId)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de transacción inválido."));

        Transaccion transaccion = Transaccion.builder()
                .usuario(usuario)
                .tipoTransaccion(tipoTransaccion)
                .descripcion(dto.getDescripcion())
                .monto(dto.getMonto())
                .fecha(LocalDate.now())
                .build();

        Transaccion guardada = transaccionRepository.save(transaccion);
        return mapToDTO(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransaccionResponseDTO> listarPorUsuario(Integer usuarioId) {
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new ResourceNotFoundException("El usuario especificado no existe.");
        }
        return transaccionRepository.findByUsuarioId(usuarioId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TransaccionResponseDTO obtenerPorId(Integer id) {
        Transaccion transaccion = transaccionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("La transacción no existe."));
        return mapToDTO(transaccion);
    }

    @Override
    @Transactional
    public TransaccionResponseDTO actualizarTransaccion(Integer id, TransaccionRequestDTO dto) {
        Transaccion transaccion = transaccionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("La transacción especificada no existe."));

        if (dto.getUsuarioId() != null) {
            Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                    .orElseThrow(() -> new ResourceNotFoundException("El usuario especificado no existe."));
            transaccion.setUsuario(usuario);
        }

        Integer tipoId = (dto.getTipoTransaccionId() != null)
                ? dto.getTipoTransaccionId()
                : clasificadorService.predecirTipoTransaccion(dto.getDescripcion());

        TipoTransaccion tipoTransaccion = tipoTransaccionRepository.findById(tipoId)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de transacción inválido."));

        transaccion.setDescripcion(dto.getDescripcion());
        transaccion.setMonto(dto.getMonto());
        transaccion.setTipoTransaccion(tipoTransaccion);

        Transaccion actualizada = transaccionRepository.save(transaccion);
        return mapToDTO(actualizada);
    }

    @Override
    @Transactional
    public void eliminarTransaccion(Integer id) {
        if (!transaccionRepository.existsById(id)) {
            throw new ResourceNotFoundException("La transacción especificada no existe.");
        }
        transaccionRepository.deleteById(id);
    }

    private TransaccionResponseDTO mapToDTO(Transaccion t) {
        return TransaccionResponseDTO.builder()
                .id(t.getId())
                .descripcion(t.getDescripcion())
                .monto(t.getMonto())
                .fecha(t.getFecha())
                .tipoTransaccion(t.getTipoTransaccion().getNombre())
                .usuarioId(t.getUsuario().getId())
                .build();
    }

}
