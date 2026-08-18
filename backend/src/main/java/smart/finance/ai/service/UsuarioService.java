package smart.finance.ai.service;

import smart.finance.ai.dto.request.UsuarioCreateRequest;
import smart.finance.ai.dto.request.UsuarioUpdateRequest;
import smart.finance.ai.dto.response.UsuarioResponse;
import smart.finance.ai.entity.Usuario;

import java.util.List;

public interface UsuarioService {

    Usuario findUserByJwtToken(String jwt);

    Usuario findUserByEmail(String email);

    UsuarioResponse crear(UsuarioCreateRequest request);

    List<UsuarioResponse> listar();

    UsuarioResponse obtenerPorId(Integer id);

    UsuarioResponse actualizar(Integer id, UsuarioUpdateRequest request);

    void eliminar(Integer id);
}

