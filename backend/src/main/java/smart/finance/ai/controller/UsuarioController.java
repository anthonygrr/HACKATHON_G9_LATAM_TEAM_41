package smart.finance.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import smart.finance.ai.dto.request.UsuarioCreateRequest;
import smart.finance.ai.dto.request.UsuarioUpdateRequest;
import smart.finance.ai.dto.response.UsuarioResponse;
import smart.finance.ai.service.UsuarioService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponseDTO<UsuarioResponse>> listar(
            @Parameter(description = "Filtro parcial por nombre o apellidos (búsqueda insensible a mayúsculas).")
            @RequestParam(required = false) String nombre,
            @Parameter(description = "Filtro parcial por correo electrónico (búsqueda insensible a mayúsculas).")
            @RequestParam(required = false) String correo,
            @Parameter(description = "Fecha de nacimiento inicial del rango (formato yyyy-MM-dd).")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaNacimientoInicio,
            @Parameter(description = "Fecha de nacimiento final del rango (formato yyyy-MM-dd).")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaNacimientoFin,
            @Parameter(description = "Número de página (empieza en 0).")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página permitido: 10, 20 o 30 (por defecto 10).")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Campo de ordenación y dirección, por ejemplo: nombre,asc.")
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(usuarioService.listar(
                nombre, correo, fechaNacimientoInicio, fechaNacimientoFin, page, size, sort));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(usuarioService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody UsuarioUpdateRequest request) {
        return ResponseEntity.ok(usuarioService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        usuarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
