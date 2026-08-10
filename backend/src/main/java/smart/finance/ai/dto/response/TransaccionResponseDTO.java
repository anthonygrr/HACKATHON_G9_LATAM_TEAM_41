package smart.finance.ai.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class TransaccionResponseDTO {
    private Integer id;
    private String descripcion;
    private String categoria;
    private Integer idCategoria;
    private BigDecimal monto;
    private LocalDate fecha;
    private String tipoTransaccion;
    private BigDecimal probabilidad;
    private Integer usuarioId;
    private String nombre;
    private String apellidoPaterno;
    private String correo;
}
