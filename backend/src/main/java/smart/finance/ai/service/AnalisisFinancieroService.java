package smart.finance.ai.service;

import smart.finance.ai.dto.request.AnalisisFinancieroRequest;
import smart.finance.ai.dto.response.AnalisisFinancieroResponse;
import smart.finance.ai.dto.response.AnalisisFinancieroResumenResponse;

import java.util.List;

public interface AnalisisFinancieroService {

    AnalisisFinancieroResponse crear(AnalisisFinancieroRequest request);

    List<AnalisisFinancieroResumenResponse> historial(Integer usuarioId);

    AnalisisFinancieroResponse obtener(Integer id);

    AnalisisFinancieroResponse actualizar(Integer id, AnalisisFinancieroRequest request);

    void eliminar(Integer id);
}