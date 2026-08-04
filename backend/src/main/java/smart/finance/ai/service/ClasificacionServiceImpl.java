package smart.finance.ai.service;

import org.springframework.stereotype.Service;
import smart.finance.ai.dto.response.TransaccionResponseDTO;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class ClasificacionServiceImpl implements ClasificacionService {

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

    /** Devuelve el id de categoria_gasto (1-8) que mejor corresponde a la descripcion. */
    @Override
    public int clasificar(String descripcion) {
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

    /** Clasifica y devuelve respaldo con la probabilidad de la regla aplicada. */
    @Override
    public TransaccionResponseDTO clasificarConProbabilidad(String descripcion) {
        int categoria = clasificar(descripcion);
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