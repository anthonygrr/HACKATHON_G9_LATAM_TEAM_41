package smart.finance.ai.dto.response;

import java.time.LocalDate;

public record UsuarioResponse(
        Integer id,
        String nombre,
        String apellidoPaterno,
        String apellidoMaterno,
        LocalDate fechaNacimiento,
        String correo,
        String rol
) {
}
