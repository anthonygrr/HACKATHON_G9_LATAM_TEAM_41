package smart.finance.ai.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Respuesta de {@code POST /analisis-financiero} de la API de Machine Learning.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MlAnalisisFinancieroResponse {

    @JsonProperty("perfil_financiero")
    private String perfilFinanciero;

    private BigDecimal probabilidad;

    @JsonProperty("resumen_gastos")
    private Map<String, BigDecimal> resumenGastos;

    private List<String> recomendaciones;
}