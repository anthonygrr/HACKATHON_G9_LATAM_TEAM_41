package smart.finance.ai.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import smart.finance.ai.entity.Usuario;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class UsuarioSpecification {

    private UsuarioSpecification() {
    }

    public static Specification<Usuario> conFiltros(String nombre, String correo,
                                                    LocalDate fechaNacimientoInicio, LocalDate fechaNacimientoFin) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();

            if (nombre != null && !nombre.isBlank()) {
                String patron = "%" + nombre.toLowerCase() + "%";
                predicados.add(cb.or(
                        cb.like(cb.lower(root.get("nombre")), patron),
                        cb.like(cb.lower(root.get("apellidoPaterno")), patron),
                        cb.like(cb.lower(root.get("apellidoMaterno")), patron)
                ));
            }

            if (correo != null && !correo.isBlank()) {
                predicados.add(cb.like(cb.lower(root.get("correo")),
                        "%" + correo.toLowerCase() + "%"));
            }

            if (fechaNacimientoInicio != null && fechaNacimientoFin != null) {
                predicados.add(cb.between(root.get("fechaNacimiento"), fechaNacimientoInicio, fechaNacimientoFin));
            } else if (fechaNacimientoInicio != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("fechaNacimiento"), fechaNacimientoInicio));
            } else if (fechaNacimientoFin != null) {
                predicados.add(cb.lessThanOrEqualTo(root.get("fechaNacimiento"), fechaNacimientoFin));
            }

            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }
}