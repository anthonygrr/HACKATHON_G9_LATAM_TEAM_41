package smart.finance.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import smart.finance.ai.dto.common.PageResponseDTO;
import smart.finance.ai.dto.request.TransaccionRequestDTO;
import smart.finance.ai.dto.response.TransaccionResponseDTO;
import smart.finance.ai.service.TransaccionService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transacciones")
@RequiredArgsConstructor
public class TransaccionController {

    private final TransaccionService transaccionService;

    @PostMapping
    public ResponseEntity<List<TransaccionResponseDTO>> crear(@Valid @RequestBody List<TransaccionRequestDTO> dtos) {
        List<TransaccionResponseDTO> response = transaccionService.crearTransacciones(dtos);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Listar transacciones",
            description = "Devuelve las transacciones del usuario autenticado con paginación y filtros opcionales "
                    + "por descripción, tipo y rango de fechas.")
    @GetMapping
    public ResponseEntity<PageResponseDTO<TransaccionResponseDTO>> listar(
            @Parameter(description = "Identificador del usuario. Si no se envía, se usa el usuario autenticado.")
            @RequestParam(required = false) Integer usuarioId,
            @Parameter(description = "Filtro parcial de la descripción (búsqueda insensible a mayúsculas).")
            @RequestParam(required = false) String descripcion,
            @Parameter(description = "Filtro por tipo de transacción: INGRESO o GASTO.")
            @RequestParam(required = false) String tipo,
            @Parameter(description = "Fecha de inicio del rango (formato yyyy-MM-dd).")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @Parameter(description = "Fecha de fin del rango (formato yyyy-MM-dd).")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @Parameter(description = "Número de página (empieza en 0).")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página permitido: 10, 20 o 30 (por defecto 10).")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Campo de ordenación y dirección, por ejemplo: fecha,desc.")
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(transaccionService.listar(
                usuarioId, descripcion, tipo, fechaInicio, fechaFin, page, size, sort));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransaccionResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(transaccionService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransaccionResponseDTO> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody TransaccionRequestDTO dto) {
        TransaccionResponseDTO response = transaccionService.actualizarTransaccion(id, dto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        transaccionService.eliminarTransaccion(id);
        return ResponseEntity.noContent().build();
    }
}
