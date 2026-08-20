package smart.finance.ai.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransaccionRequestDTO {

    private Integer usuarioId;

    @NotBlank(message = "La descripción no puede estar vacía")
    @Size(max = 255, message = "La descripción excede los 255 caracteres")
    private String descripcion;

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser un número positivo mayor a cero")
    private BigDecimal monto;

    private Integer tipoTransaccionId;

    @Schema(
            description = "Fecha de la transacción en formato yyyy-MM-dd. Es opcional: si no se envía, " +
                    "se usa la fecha actual del sistema (LocalDate.now()).",
            example = "2026-08-20",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private LocalDate fecha;
}
