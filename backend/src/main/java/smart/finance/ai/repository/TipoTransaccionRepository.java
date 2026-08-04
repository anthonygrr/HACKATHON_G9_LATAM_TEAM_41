package smart.finance.ai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import smart.finance.ai.entity.TipoTransaccion;

@Repository
public interface TipoTransaccionRepository extends JpaRepository<TipoTransaccion, Integer> {
}
