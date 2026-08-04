package smart.finance.ai.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransaccionRequestDTO {

    @NotNull(message = "El ID de usuario es obligatorio")
    private Integer usuarioId;

    @NotBlank(message = "La descripción no puede estar vacía")
    @Size(max = 255, message = "La descripción excede los 255 caracteres")
    private String descripcion;

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser un número positivo mayor a cero")
    private BigDecimal monto;

    private Integer tipoTransaccionId;
}
