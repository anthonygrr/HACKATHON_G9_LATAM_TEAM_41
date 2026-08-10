package smart.finance.ai.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import smart.finance.ai.dto.request.AnalisisFinancieroRequest;
import smart.finance.ai.dto.response.AnalisisFinancieroResponse;
import smart.finance.ai.dto.response.AnalisisFinancieroResumenResponse;
import smart.finance.ai.service.AnalisisFinancieroService;

import java.util.List;

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

    @GetMapping
    public ResponseEntity<List<AnalisisFinancieroResumenResponse>> historial(
            @RequestParam Integer usuarioId) {
        return ResponseEntity.ok(service.historial(usuarioId));
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