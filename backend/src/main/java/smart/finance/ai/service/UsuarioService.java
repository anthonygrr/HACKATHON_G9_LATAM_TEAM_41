package smart.finance.ai.service;

import smart.finance.ai.entity.Usuario;

public interface UsuarioService {

    Usuario findUserByJwtToken(String jwt);

    Usuario findUserByEmail(String email);
}

