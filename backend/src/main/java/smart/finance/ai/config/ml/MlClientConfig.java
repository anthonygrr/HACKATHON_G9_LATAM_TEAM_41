package smart.finance.ai.config.ml;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Cliente HTTP hacia la API de Machine Learning (data-science/ml-api).
 * Es una llamada saliente: el backend NO expone endpoints nuevos, solo invoca
 * al servicio DS y recibe el JSON de respuesta de forma sincrona.
 */
@Configuration
public class MlClientConfig {

    @Bean
    public RestClient mlRestClient(
            @Value("${ml.service.base-url:http://localhost:8000}") String baseUrl,
            @Value("${ml.service.api-key:}") String apiKey,
            @Value("${ml.service.timeout-ms:5000}") int timeoutMs) {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(requestFactory);

        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader("X-API-Key", apiKey);
        }

        return builder.build();
    }
}