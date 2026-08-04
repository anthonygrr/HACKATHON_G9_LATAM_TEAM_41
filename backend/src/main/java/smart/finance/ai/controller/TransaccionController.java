package smart.finance.ai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import smart.finance.ai.dto.request.TransaccionRequestDTO;
import smart.finance.ai.dto.response.TransaccionResponseDTO;
import smart.finance.ai.service.TransaccionService;
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

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<TransaccionResponseDTO>> listarPorUsuario(@PathVariable Integer usuarioId) {
        return ResponseEntity.ok(transaccionService.listarPorUsuario(usuarioId));
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
