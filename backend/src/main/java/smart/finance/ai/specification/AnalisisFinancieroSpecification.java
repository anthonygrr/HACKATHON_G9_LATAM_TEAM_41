package smart.finance.ai.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import smart.finance.ai.entity.AnalisisFinanciero;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AnalisisFinancieroSpecification {

    private AnalisisFinancieroSpecification() {
    }

    public static Specification<AnalisisFinanciero> conFiltros(Integer usuarioId, String salud,
                                                               LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();

            if (usuarioId != null) {
                predicados.add(cb.equal(root.get("usuario").get("id"), usuarioId));
            }

            if (salud != null && !salud.isBlank()) {
                predicados.add(cb.equal(cb.lower(root.get("saludFinanciera").get("nombre")),
                        salud.toLowerCase()));
            }

            if (fechaInicio != null && fechaFin != null) {
                predicados.add(cb.between(root.get("fechaGeneracion"), fechaInicio, fechaFin));
            } else if (fechaInicio != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("fechaGeneracion"), fechaInicio));
            } else if (fechaFin != null) {
                predicados.add(cb.lessThanOrEqualTo(root.get("fechaGeneracion"), fechaFin));
            }

            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }
}