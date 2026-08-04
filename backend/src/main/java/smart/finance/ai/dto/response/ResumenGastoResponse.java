package smart.finance.ai.dto.response;

import java.math.BigDecimal;

public record ResumenGastoResponse(
        String categoria,
        BigDecimal montoTotal
) {}