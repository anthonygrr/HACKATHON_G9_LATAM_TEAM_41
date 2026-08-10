package smart.finance.ai.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record AnalisisFinancieroRequest(
        @NotNull Integer usuarioId,
        @NotNull Integer mes,
        @NotNull Integer anio,
        @NotNull @Positive BigDecimal ingresoMensual,
        @NotNull @Positive BigDecimal nivelEndeudamiento,
        @NotBlank String frecuenciaAhorro,
        @NotEmpty @Valid List<TransaccionAnalisisRequest> transacciones
) {}