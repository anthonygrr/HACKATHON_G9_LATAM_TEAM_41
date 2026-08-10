package smart.finance.ai.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AnalisisFinancieroResumenResponse(
        Integer id,
        BigDecimal ingresoMensual,
        BigDecimal nivelEndeudamiento,
        String frecuenciaAhorro,
        Integer mes,
        Integer anio,
        LocalDateTime fechaGeneracion,
        String saludFinanciera,
        BigDecimal probabilidad
) {}