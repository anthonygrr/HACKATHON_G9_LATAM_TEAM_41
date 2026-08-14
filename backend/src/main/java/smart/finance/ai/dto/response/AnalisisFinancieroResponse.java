package smart.finance.ai.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AnalisisFinancieroResponse(
        Integer id,
        Integer usuarioId,
        BigDecimal ingresoMensual,
        BigDecimal nivelEndeudamiento,
        String frecuenciaAhorro,
        Integer mes,
        Integer anio,
        LocalDateTime fechaGeneracion,
        String saludFinanciera,
        BigDecimal probabilidad,
        List<ResumenGastoResponse> resumenGastos,
        List<String> recomendaciones
) {}