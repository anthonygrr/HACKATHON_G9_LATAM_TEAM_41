package smart.finance.ai.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import smart.finance.ai.entity.Transaccion;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TransaccionSpecification {

    private TransaccionSpecification() {
    }

    public static Specification<Transaccion> conFiltros(Integer usuarioId, String descripcion,
                                                        String tipo, LocalDate fechaInicio, LocalDate fechaFin) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();

            if (usuarioId != null) {
                predicados.add(cb.equal(root.get("usuario").get("id"), usuarioId));
            }

            if (descripcion != null && !descripcion.isBlank()) {
                predicados.add(cb.like(cb.lower(root.get("descripcion")),
                        "%" + descripcion.toLowerCase() + "%"));
            }

            if (tipo != null && !tipo.isBlank()) {
                predicados.add(cb.equal(cb.lower(root.get("tipoTransaccion").get("nombre")),
                        tipo.toLowerCase()));
            }

            if (fechaInicio != null && fechaFin != null) {
                predicados.add(cb.between(root.get("fecha"), fechaInicio, fechaFin));
            } else if (fechaInicio != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("fecha"), fechaInicio));
            } else if (fechaFin != null) {
                predicados.add(cb.lessThanOrEqualTo(root.get("fecha"), fechaFin));
            }

            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }
}