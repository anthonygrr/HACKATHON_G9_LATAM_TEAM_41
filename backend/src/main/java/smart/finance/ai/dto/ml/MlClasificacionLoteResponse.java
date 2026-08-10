package smart.finance.ai.dto.ml;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Respuesta de la API DS para {@code POST /clasificar-transacciones} (lote).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MlClasificacionLoteResponse {

    private List<MlClasificacionResponse> transacciones;
}