package smart.finance.ai.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "El correo es obligatorio")
        String correo,
        @NotBlank(message = "La contraseña es obligatoria")
        String contrasena
) {
}

