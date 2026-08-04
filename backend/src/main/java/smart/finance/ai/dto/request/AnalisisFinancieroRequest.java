package smart.finance.ai.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AnalisisFinancieroRequest(
        @NotNull Integer usuarioId,
        @NotNull Integer mes,
        @NotNull Integer anio,
        @NotEmpty @Valid List<TransaccionAnalisisRequest> transacciones
) {}