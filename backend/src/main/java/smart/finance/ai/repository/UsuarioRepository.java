package smart.finance.ai.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import smart.finance.ai.entity.Usuario;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    public Optional<Usuario> findByCorreo(String username);

}