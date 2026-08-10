package smart.finance.ai.service;

import smart.finance.ai.dto.response.TransaccionResponseDTO;

import java.util.List;
import java.util.Map;

public interface ClasificacionService {

    /** Devuelve el id de categoria_gasto (1-8) que mejor corresponde a la descripcion. */
    int clasificar(String descripcion);

    /** Predice el tipo de transaccion (1=INGRESO, 2=GASTO) a partir de la descripcion. */
    int predecirTipoTransaccion(String descripcion);

    /** Clasifica y devuelve categoria, idCategoria y probabilidad. */
    TransaccionResponseDTO clasificarConProbabilidad(String descripcion);

    /**
     * Clasifica un lote de descripciones en una sola llamada al modelo.
     * Devuelve un mapa descripcion -> clasificacion; los items que no pudo
     * resolver el modelo usan las reglas locales como respaldo.
     */
    Map<String, TransaccionResponseDTO> clasificarConProbabilidadLote(List<String> descripciones);
}