package smart.finance.ai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import smart.finance.ai.entity.TipoTransaccion;

import java.util.Optional;

@Repository
public interface TipoTransaccionRepository extends JpaRepository<TipoTransaccion, Integer> {
    Optional<TipoTransaccion> findByNombre(String nombre);
}
