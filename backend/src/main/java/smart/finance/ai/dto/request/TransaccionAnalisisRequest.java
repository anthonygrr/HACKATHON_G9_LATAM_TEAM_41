package smart.finance.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransaccionAnalisisRequest(
        @NotBlank String descripcion,
        @NotNull @Positive BigDecimal monto
) {}