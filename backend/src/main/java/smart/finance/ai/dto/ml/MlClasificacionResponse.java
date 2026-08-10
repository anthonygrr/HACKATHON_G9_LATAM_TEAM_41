package smart.finance.ai.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta de la API DS para {@code POST /clasificar-transaccion}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MlClasificacionResponse {

    private String descripcion;
    private String categoria;

    @JsonProperty("categoria_nombre")
    private String categoriaNombre;

    private Double probabilidad;
}