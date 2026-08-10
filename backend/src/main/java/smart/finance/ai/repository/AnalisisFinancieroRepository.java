package smart.finance.ai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import smart.finance.ai.entity.AnalisisFinanciero;

import java.util.List;

public interface AnalisisFinancieroRepository extends JpaRepository<AnalisisFinanciero, Integer> {

    @Query("SELECT a FROM AnalisisFinanciero a WHERE a.usuario.id = :usuarioId ORDER BY a.fechaGeneracion DESC")
    List<AnalisisFinanciero> findByUsuarioOrderByFechaGeneracionDesc(@Param("usuarioId") Integer usuarioId);
}