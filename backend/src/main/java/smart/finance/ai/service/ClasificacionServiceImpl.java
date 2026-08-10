package smart.finance.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import smart.finance.ai.dto.ml.MlClasificacionResponse;
import smart.finance.ai.dto.response.TransaccionResponseDTO;
import smart.finance.ai.util.CategoriaGastoMapper;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClasificacionServiceImpl implements ClasificacionService {

    private final MlApiClient mlApiClient;

    private static final Map<String, Integer> PALABRAS_CLAVE = new LinkedHashMap<>();

    static {
        // Alimentación (1)
        PALABRAS_CLAVE.put("supermercado", 1);
        PALABRAS_CLAVE.put("mercado", 1);
        PALABRAS_CLAVE.put("restaurante", 1);
        PALABRAS_CLAVE.put("comida", 1);
        // Transporte (2)
        PALABRAS_CLAVE.put("combustible", 2);
        PALABRAS_CLAVE.put("gasolina", 2);
        PALABRAS_CLAVE.put("uber", 2);
        PALABRAS_CLAVE.put("taxi", 2);
        PALABRAS_CLAVE.put("transporte", 2);
        // Salud (3)
        PALABRAS_CLAVE.put("farmacia", 3);
        PALABRAS_CLAVE.put("hospital", 3);
        PALABRAS_CLAVE.put("doctor", 3);
        PALABRAS_CLAVE.put("medico", 3);
        // Vivienda (4)
        PALABRAS_CLAVE.put("renta", 4);
        PALABRAS_CLAVE.put("alquiler", 4);
        PALABRAS_CLAVE.put("hipoteca", 4);
        PALABRAS_CLAVE.put("vivienda", 4);
        // Educación (5)
        PALABRAS_CLAVE.put("colegio", 5);
        PALABRAS_CLAVE.put("colegiatura", 5);
        PALABRAS_CLAVE.put("universidad", 5);
        PALABRAS_CLAVE.put("curso", 5);
        // Ocio (6)
        PALABRAS_CLAVE.put("streaming", 6);
        PALABRAS_CLAVE.put("netflix", 6);
        PALABRAS_CLAVE.put("spotify", 6);
        PALABRAS_CLAVE.put("cine", 6);
        PALABRAS_CLAVE.put("ocio", 6);
        PALABRAS_CLAVE.put("entretenimiento", 6);
        // Servicios (7)
        PALABRAS_CLAVE.put("luz", 7);
        PALABRAS_CLAVE.put("agua", 7);
        PALABRAS_CLAVE.put("internet", 7);
        PALABRAS_CLAVE.put("telefono", 7);
    }

    /** Devuelve el id de categoria_gasto (1-8) que mejor corresponde a la descripcion.
     *  Se delega al modelo de data-science; si el servicio ML no esta disponible,
     *  se degrada a las reglas locales (PALABRAS_CLAVE). */
    @Override
    public int clasificar(String descripcion) {
        Optional<MlClasificacionResponse> mRespuesta = mlApiClient.clasificar(descripcion);
        if (mRespuesta.isPresent() && mRespuesta.get().getCategoria() != null) {
            Integer id = CategoriaGastoMapper.idDesdeSlug(mRespuesta.get().getCategoria());
            if (id != null) {
                return id;
            }
        }
        return clasificarPorRegla(descripcion);
    }

    private int clasificarPorRegla(String descripcion) {
        String textoNormalizado = descripcion == null
                ? ""
                : descripcion.toLowerCase(Locale.forLanguageTag("es"));
        for (Map.Entry<String, Integer> entry : PALABRAS_CLAVE.entrySet()) {
            if (textoNormalizado.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return 8; // Otras
    }

    /**
     * Predice el tipo de transaccion (1=INGRESO, 2=GASTO) a partir de la descripcion.
     * Si no se puede inferir un ingreso, se devuelve el id por defecto de GASTO (2).
     */
    @Override
    public int predecirTipoTransaccion(String descripcion) {
        if (descripcion == null) {
            return 2; // Default a GASTO
        }
        String desc = descripcion.toLowerCase(Locale.forLanguageTag("es"));
        if (desc.contains("nomina") || desc.contains("ingreso") || desc.contains("salario")
                || desc.contains("sueldo") || desc.contains("deposito")) {
            return 1; // ID 1 = INGRESO
        }
        return 2; // ID 2 = GASTO
    }

    /**
     * Clasifica contra el modelo y devuelve la categoria con la probabilidad real.
     * Si el servicio ML no esta disponible, usa las reglas locales con una
     * probabilidad fija como respaldo.
     */
    @Override
    public TransaccionResponseDTO clasificarConProbabilidad(String descripcion) {
        Optional<MlClasificacionResponse> mRespuesta = mlApiClient.clasificar(descripcion);
        if (mRespuesta.isPresent() && mRespuesta.get().getCategoria() != null) {
            return toDtoModelo(mRespuesta.get());
        }
        return toDtoRegla(descripcion);
    }

    @Override
    public Map<String, TransaccionResponseDTO> clasificarConProbabilidadLote(List<String> descripciones) {
        Map<String, TransaccionResponseDTO> resultado = new LinkedHashMap<>();

        mlApiClient.clasificarLote(descripciones).ifPresent(lista ->
                lista.forEach(respuesta -> {
                    if (respuesta.getDescripcion() != null) {
                        resultado.merge(respuesta.getDescripcion(),
                                toDtoModelo(respuesta), (a, b) -> a);
                    }
                }));

        for (String descripcion : descripciones) {
            if (descripcion != null && !resultado.containsKey(descripcion)) {
                resultado.put(descripcion, toDtoRegla(descripcion));
            }
        }
        return resultado;
    }

    private TransaccionResponseDTO toDtoModelo(MlClasificacionResponse respuesta) {
        Integer id = CategoriaGastoMapper.idDesdeSlug(respuesta.getCategoria());
        BigDecimal probabilidad = respuesta.getProbabilidad() == null
                ? new BigDecimal("0.870")
                : BigDecimal.valueOf(respuesta.getProbabilidad());
        return TransaccionResponseDTO.builder()
                .categoria(CategoriaGastoMapper.nombreDesdeSlug(respuesta.getCategoria()))
                .idCategoria(id)
                .probabilidad(probabilidad)
                .build();
    }

    private TransaccionResponseDTO toDtoRegla(String descripcion) {
        int categoria = clasificarPorRegla(descripcion);
        BigDecimal probabilidad = categoria == 8
                ? new BigDecimal("0.500")
                : new BigDecimal("0.870");
        return TransaccionResponseDTO.builder()
                .categoria(nombreCategoria(categoria))
                .idCategoria(categoria)
                .probabilidad(probabilidad)
                .build();
    }

    private String nombreCategoria(int id) {
        return switch (id) {
            case 1 -> "Alimentación";
            case 2 -> "Transporte";
            case 3 -> "Salud";
            case 4 -> "Vivienda";
            case 5 -> "Educación";
            case 6 -> "Ocio";
            case 7 -> "Servicios";
            default -> "Otras";
        };
    }
}