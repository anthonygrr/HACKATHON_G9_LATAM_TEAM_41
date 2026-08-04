package smart.finance.ai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import smart.finance.ai.entity.Recomendacion;

import java.util.List;

public interface RecomendacionRepository extends JpaRepository<Recomendacion, Integer> {

    List<Recomendacion> findByAnalisisFinancieroId(Integer idAnalisis);
}