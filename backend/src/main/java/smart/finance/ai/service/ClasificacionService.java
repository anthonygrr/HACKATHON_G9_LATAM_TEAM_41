package smart.finance.ai.service;

import smart.finance.ai.dto.response.TransaccionResponseDTO;

public interface ClasificacionService {

    /** Devuelve el id de categoria_gasto (1-8) que mejor corresponde a la descripcion. */
    int clasificar(String descripcion);

    /** Predice el tipo de transaccion (1=INGRESO, 2=GASTO) a partir de la descripcion. */
    int predecirTipoTransaccion(String descripcion);

    /** Clasifica y devuelve categoria, idCategoria y probabilidad. */
    TransaccionResponseDTO clasificarConProbabilidad(String descripcion);
}