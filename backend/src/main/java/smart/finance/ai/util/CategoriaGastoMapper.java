package smart.finance.ai.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce los slugs usados por el modelo de data-science a los id/ids que
 * maneja la base de datos y el dominio del backend.
 */
public final class CategoriaGastoMapper {

    private static final Map<String, Integer> SLUG_A_ID = new LinkedHashMap<>();
    private static final Map<String, String> SLUG_A_NOMBRE = new LinkedHashMap<>();

    static {
        SLUG_A_ID.put("alimentacion", 1);
        SLUG_A_ID.put("transporte", 2);
        SLUG_A_ID.put("salud", 3);
        SLUG_A_ID.put("vivienda", 4);
        SLUG_A_ID.put("educacion", 5);
        SLUG_A_ID.put("ocio", 6);
        SLUG_A_ID.put("servicios", 7);
        SLUG_A_ID.put("otros", 8);

        SLUG_A_NOMBRE.put("alimentacion", "Alimentación");
        SLUG_A_NOMBRE.put("transporte", "Transporte");
        SLUG_A_NOMBRE.put("salud", "Salud");
        SLUG_A_NOMBRE.put("vivienda", "Vivienda");
        SLUG_A_NOMBRE.put("educacion", "Educación");
        SLUG_A_NOMBRE.put("ocio", "Ocio");
        SLUG_A_NOMBRE.put("servicios", "Servicios");
        SLUG_A_NOMBRE.put("otros", "Otras");
    }

    private CategoriaGastoMapper() {
    }

    public static Integer idDesdeSlug(String slug) {
        return slug == null ? null : SLUG_A_ID.get(slug.trim().toLowerCase());
    }

    public static String nombreDesdeSlug(String slug) {
        if (slug == null) {
            return null;
        }
        return SLUG_A_NOMBRE.getOrDefault(slug.trim().toLowerCase(), slug);
    }

    /**
     * Mapea el perfil financiero devuelto por el modelo a su id en
     * {@code salud_financiera} (1=Saludable, 2=Moderada, 3=En riesgo).
     */
    public static Integer idSaludFinanciera(String perfilFinanciero) {
        if (perfilFinanciero == null) {
            return null;
        }
        String p = perfilFinanciero.trim();
        if (p.equalsIgnoreCase("Saludable")) {
            return 1;
        }
        if (p.equalsIgnoreCase("En riesgo")) {
            return 3;
        }
        if (p.equalsIgnoreCase("En observacion")) {
            return 2;
        }
        if (p.equalsIgnoreCase("Moderada") || p.equalsIgnoreCase("En observación")) {
            return 2;
        }
        return null;
    }
}