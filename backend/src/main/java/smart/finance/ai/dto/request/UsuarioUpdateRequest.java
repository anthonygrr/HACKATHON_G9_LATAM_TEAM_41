package smart.finance.ai.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UsuarioUpdateRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
        String nombre,

        @NotBlank(message = "El apellido paterno es obligatorio")
        @Size(min = 2, max = 100, message = "El apellido paterno debe tener entre 2 y 100 caracteres")
        String apellidoPaterno,

        @Size(max = 100, message = "El apellido materno no debe superar los 100 caracteres")
        String apellidoMaterno,

        LocalDate fechaNacimiento,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo debe tener un formato valido")
        @Size(max = 255, message = "El correo no debe superar los 255 caracteres")
        String correo,

        @NotNull(message = "El rol es obligatorio")
        Integer rolId
) {
}
