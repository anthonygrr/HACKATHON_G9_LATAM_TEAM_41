package smart.finance.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import smart.finance.ai.dto.common.PageResponseDTO;
import smart.finance.ai.dto.request.AnalisisFinancieroRequest;
import smart.finance.ai.dto.response.AnalisisFinancieroResponse;
import smart.finance.ai.dto.response.AnalisisFinancieroResumenResponse;
import smart.finance.ai.service.AnalisisFinancieroService;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/analisis-financiero")
public class AnalisisFinancieroController {

    private final AnalisisFinancieroService service;

    public AnalisisFinancieroController(AnalisisFinancieroService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AnalisisFinancieroResponse> crear(
            @Valid @RequestBody AnalisisFinancieroRequest request) {
        return new ResponseEntity<>(service.crear(request), HttpStatus.CREATED);
    }

    @Operation(
            summary = "Historial de análisis financieros",
            description = "Devuelve el historial de análisis del usuario autenticado con paginación y filtros "
                    + "opcionales por salud financiera y rango de fechas.")
    @GetMapping
    public ResponseEntity<PageResponseDTO<AnalisisFinancieroResumenResponse>> historial(
            @Parameter(description = "Identificador del usuario. Si no se envía, se usa el usuario autenticado.")
            @RequestParam(required = false) Integer usuarioId,
            @Parameter(description = "Filtro por salud financiera: SALUDABLE, MODERADA o EN_RIESGO.")
            @RequestParam(required = false) String salud,
            @Parameter(description = "Fecha y hora de inicio del rango (formato yyyy-MM-dd'T'HH:mm:ss).")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @Parameter(description = "Fecha y hora de fin del rango (formato yyyy-MM-dd'T'HH:mm:ss).")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,
            @Parameter(description = "Número de página (empieza en 0).")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página permitido: 10, 20 o 30 (por defecto 10).")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Campo de ordenación y dirección, por ejemplo: fechaGeneracion,desc.")
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(service.historial(usuarioId, salud, fechaInicio, fechaFin, page, size, sort));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnalisisFinancieroResponse> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AnalisisFinancieroResponse> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody AnalisisFinancieroRequest request) {
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}