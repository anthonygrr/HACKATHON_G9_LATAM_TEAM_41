package smart.finance.ai.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import smart.finance.ai.dto.request.TransaccionAnalisisRequest;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cuerpo enviado a {@code POST /analisis-financiero} de la API de Machine Learning.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MlAnalisisFinancieroRequest {

    @NotNull
    @JsonProperty("ingreso_mensual")
    private BigDecimal ingresoMensual;

    @NotNull
    @JsonProperty("nivel_endeudamiento")
    private BigDecimal nivelEndeudamiento;

    @NotBlank
    @JsonProperty("frecuencia_ahorro")
    private String frecuenciaAhorro;

    @NotEmpty
    @Valid
    private List<TransaccionAnalisisRequest> transacciones;
}