package smart.finance.ai.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record AnalisisFinancieroRequest(
        Integer usuarioId,
        @NotNull Integer mes,
        @NotNull Integer anio,
        @NotNull @Positive BigDecimal ingresoMensual,
        @NotNull @Positive BigDecimal nivelEndeudamiento,
        @NotBlank String frecuenciaAhorro,
        @Schema(
                description = "Transacciones a analizar. Opcional: si es null o vacío, se consultan las " +
                        "transacciones persistidas del usuario autenticado en la base de datos (solo en POST).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Valid List<TransaccionAnalisisRequest> transacciones
) {}