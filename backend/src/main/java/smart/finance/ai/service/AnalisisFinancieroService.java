package smart.finance.ai.service;

import smart.finance.ai.dto.common.PageResponseDTO;
import smart.finance.ai.dto.request.AnalisisFinancieroRequest;
import smart.finance.ai.dto.response.AnalisisFinancieroResponse;
import smart.finance.ai.dto.response.AnalisisFinancieroResumenResponse;

import java.time.LocalDateTime;

public interface AnalisisFinancieroService {

    AnalisisFinancieroResponse crear(AnalisisFinancieroRequest request);

    PageResponseDTO<AnalisisFinancieroResumenResponse> historial(Integer usuarioId, String salud,
                                                                 LocalDateTime fechaInicio, LocalDateTime fechaFin,
                                                                 int page, int size, String sort);

    AnalisisFinancieroResponse obtener(Integer id);

    AnalisisFinancieroResponse actualizar(Integer id, AnalisisFinancieroRequest request);

    void eliminar(Integer id);
}