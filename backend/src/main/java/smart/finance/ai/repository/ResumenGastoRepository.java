package smart.finance.ai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import smart.finance.ai.entity.ResumenGasto;

import java.util.List;

public interface ResumenGastoRepository extends JpaRepository<ResumenGasto, Integer> {

    List<ResumenGasto> findByClasificacionTransaccionId(Integer idClasificacion);
}