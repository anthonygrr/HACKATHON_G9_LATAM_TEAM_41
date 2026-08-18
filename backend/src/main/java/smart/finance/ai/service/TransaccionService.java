package smart.finance.ai.service;

import smart.finance.ai.dto.common.PageResponseDTO;
import smart.finance.ai.dto.request.TransaccionRequestDTO;
import smart.finance.ai.dto.response.TransaccionResponseDTO;

import java.time.LocalDate;
import java.util.List;

public interface TransaccionService {
    List<TransaccionResponseDTO> crearTransacciones(List<TransaccionRequestDTO> dtos);

    PageResponseDTO<TransaccionResponseDTO> listar(Integer usuarioId, String descripcion, String tipo,
                                                   LocalDate fechaInicio, LocalDate fechaFin,
                                                   int page, int size, String sort);

    TransaccionResponseDTO obtenerPorId(Integer id);

    TransaccionResponseDTO actualizarTransaccion(Integer id, TransaccionRequestDTO dto);

    void eliminarTransaccion(Integer id);
}