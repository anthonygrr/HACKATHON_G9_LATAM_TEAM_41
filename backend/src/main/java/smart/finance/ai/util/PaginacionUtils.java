package smart.finance.ai.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public class PaginacionUtils {

    private static final int TAMANIO_PAGINA_DEFAULT = 10;
    private static final Set<Integer> TAMANIOS_PERMITIDOS = Set.of(10, 20, 30);

    private PaginacionUtils() {
    }

    public static Pageable crearPageable(int page, int size, String sort,
                                         Set<String> camposPermitidos, String campoPorDefecto) {
        int tamanio = TAMANIOS_PERMITIDOS.contains(size) ? size : TAMANIO_PAGINA_DEFAULT;

        String campo = campoPorDefecto;
        Sort.Direction direccion = Sort.Direction.ASC;

        if (sort != null && !sort.isBlank()) {
            String[] partes = sort.split(",");
            String campoSolicitado = partes[0].trim();
            if (camposPermitidos.contains(campoSolicitado)) {
                campo = campoSolicitado;
            }
            if (partes.length > 1) {
                try {
                    direccion = Sort.Direction.fromString(partes[1].trim());
                } catch (IllegalArgumentException e) {
                    direccion = Sort.Direction.ASC;
                }
            }
        }

        return PageRequest.of(Math.max(page, 0), tamanio, Sort.by(direccion, campo));
    }
}