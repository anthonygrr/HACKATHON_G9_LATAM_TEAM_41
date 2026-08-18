package smart.finance.ai.service;

import smart.finance.ai.dto.common.PageResponseDTO;
import smart.finance.ai.dto.request.UsuarioCreateRequest;
import smart.finance.ai.dto.request.UsuarioUpdateRequest;
import smart.finance.ai.dto.response.UsuarioResponse;
import smart.finance.ai.entity.Usuario;

import java.time.LocalDate;

public interface UsuarioService {

    Usuario findUserByJwtToken(String jwt);

    Usuario findUserByEmail(String email);

    UsuarioResponse crear(UsuarioCreateRequest request);

    PageResponseDTO<UsuarioResponse> listar(String nombre, String correo,
                                            LocalDate fechaNacimientoInicio, LocalDate fechaNacimientoFin,
                                            int page, int size, String sort);

    UsuarioResponse obtenerPorId(Integer id);

    UsuarioResponse actualizar(Integer id, UsuarioUpdateRequest request);

    void eliminar(Integer id);
}