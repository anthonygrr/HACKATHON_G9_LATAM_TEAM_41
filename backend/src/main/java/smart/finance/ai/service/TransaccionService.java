package smart.finance.ai.service;

import smart.finance.ai.dto.request.TransaccionRequestDTO;
import smart.finance.ai.dto.response.TransaccionResponseDTO;
import java.util.List;

public interface TransaccionService {
    TransaccionResponseDTO crearTransaccion(TransaccionRequestDTO dto);
    List<TransaccionResponseDTO> listarPorUsuario(Integer usuarioId);
    TransaccionResponseDTO obtenerPorId(Integer id);
    TransaccionResponseDTO actualizarTransaccion(Integer id, TransaccionRequestDTO dto);
    void eliminarTransaccion(Integer id);
}
