package smart.finance.ai.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import smart.finance.ai.dto.ml.MlAnalisisFinancieroRequest;
import smart.finance.ai.dto.ml.MlAnalisisFinancieroResponse;
import smart.finance.ai.dto.ml.MlClasificacionLoteResponse;
import smart.finance.ai.dto.ml.MlClasificacionResponse;

import java.util.List;
import java.util.Optional;

/**
 * Cliente interno hacia la API de Machine Learning (data-science/ml-api).
 * Cada llamado puede fallar (servicio caido, timeout): retorna {@link Optional}
 * vacio en ese caso para que el llamador pueda degradar a la logica local.
 */
@Component
@RequiredArgsConstructor
public class MlApiClient {

    private static final Logger log = LoggerFactory.getLogger(MlApiClient.class);

    private final RestClient mlRestClient;

    public Optional<MlClasificacionResponse> clasificar(String descripcion) {
        try {
            MlClasificacionResponse res = mlRestClient.post()
                    .uri("/clasificar-transaccion")
                    .body(java.util.Map.of("descripcion", descripcion))
                    .retrieve()
                    .body(MlClasificacionResponse.class);
            return Optional.ofNullable(res);
        } catch (Exception ex) {
            log.warn("ML API /clasificar-transaccion no disponible para '{}': {}",
                    descripcion, ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Clasifica un lote de descripciones en una sola llamada.
     * Devuelve vacio si el servicio ML no esta disponible.
     */
    public Optional<List<MlClasificacionResponse>> clasificarLote(List<String> descripciones) {
        try {
            List<java.util.Map<String, String>> items = descripciones.stream()
                    .map(d -> java.util.Map.of("descripcion", d == null ? "" : d))
                    .toList();
            MlClasificacionLoteResponse res = mlRestClient.post()
                    .uri("/clasificar-transacciones")
                    .body(java.util.Map.of("transacciones", items))
                    .retrieve()
                    .body(MlClasificacionLoteResponse.class);
            if (res == null || res.getTransacciones() == null) {
                return Optional.empty();
            }
            return Optional.of(res.getTransacciones());
        } catch (Exception ex) {
            log.warn("ML API /clasificar-transacciones no disponible: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public Optional<MlAnalisisFinancieroResponse> analizarFinanciero(MlAnalisisFinancieroRequest request) {
        try {
            MlAnalisisFinancieroResponse res = mlRestClient.post()
                    .uri("/analisis-financiero")
                    .body(request)
                    .retrieve()
                    .body(MlAnalisisFinancieroResponse.class);
            return Optional.ofNullable(res);
        } catch (Exception ex) {
            log.warn("ML /analisis-financiero no disponible: {}", ex.getMessage());
            return Optional.empty();
        }
    }
}