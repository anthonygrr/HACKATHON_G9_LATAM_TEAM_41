package smart.finance.ai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import smart.finance.ai.entity.Transaccion;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransaccionRepository extends JpaRepository<Transaccion, Integer> {
    List<Transaccion> findByUsuarioId(Integer usuarioId);
    List<Transaccion> findByUsuarioIdAndFechaBetween(Integer usuarioId, LocalDate inicio, LocalDate fin);
}