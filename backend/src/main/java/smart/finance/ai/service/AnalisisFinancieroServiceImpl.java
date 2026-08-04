package smart.finance.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import smart.finance.ai.dto.request.AnalisisFinancieroRequest;
import smart.finance.ai.dto.request.TransaccionAnalisisRequest;
import smart.finance.ai.dto.response.AnalisisFinancieroResponse;
import smart.finance.ai.dto.response.AnalisisFinancieroResumenResponse;
import smart.finance.ai.dto.response.ResumenGastoResponse;
import smart.finance.ai.entity.*;
import smart.finance.ai.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalisisFinancieroServiceImpl implements AnalisisFinancieroService {

    private final AnalisisFinancieroRepository analisisRepository;
    private final ClasificacionTransaccionRepository clasificacionRepository;
    private final ResumenGastoRepository resumenRepository;
    private final RecomendacionRepository recomendacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final SaludFinancieraRepository saludFinancieraRepository;
    private final CategoriaGastoRepository categoriaGastoRepository;
    private final TipoTransaccionRepository tipoTransaccionRepository;
    private final TransaccionRepository transaccionRepository;
    private final ClasificacionService clasificacionService;

    @Override
    @Transactional
    public AnalisisFinancieroResponse crear(AnalisisFinancieroRequest request) {
        Usuario usuario = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado: " + request.usuarioId()));

        AnalisisFinanciero analisis = AnalisisFinanciero.builder()
                .usuario(usuario)
                .mes(request.mes())
                .anio(request.anio())
                .fechaGeneracion(LocalDateTime.now())
                .recomendaciones(new ArrayList<>())
                .build();

        poblar(analisis, request);

        AnalisisFinanciero guardado = analisisRepository.save(analisis);
        return toDetalleResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalisisFinancieroResumenResponse> historial(Integer usuarioId) {
        return analisisRepository.findByUsuarioOrderByFechaGeneracionDesc(usuarioId)
                .stream()
                .map(a -> new AnalisisFinancieroResumenResponse(
                        a.getId(),
                        a.getMes(),
                        a.getAnio(),
                        a.getFechaGeneracion(),
                        a.getSaludFinanciera().getNombre(),
                        a.getClasificacionTransaccion() == null
                                ? BigDecimal.ZERO
                                : a.getClasificacionTransaccion().getProbabilidad()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AnalisisFinancieroResponse obtener(Integer id) {
        AnalisisFinanciero analisis = analisisRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Analisis no encontrado: " + id));
        return toDetalleResponse(analisis);
    }

    @Override
    @Transactional
    public AnalisisFinancieroResponse actualizar(Integer id, AnalisisFinancieroRequest request) {
        AnalisisFinanciero analisis = analisisRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Analisis no encontrado: " + id));

        Usuario usuario = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado: " + request.usuarioId()));

        analisis.setUsuario(usuario);
        analisis.setMes(request.mes());
        analisis.setAnio(request.anio());
        analisis.setFechaGeneracion(LocalDateTime.now());

        if (analisis.getClasificacionTransaccion() != null) {
            analisis.getClasificacionTransaccion().getResumenesGasto().clear();
        }
        analisis.setClasificacionTransaccion(null);
        analisis.getRecomendaciones().clear();

        poblar(analisis, request);

        return toDetalleResponse(analisisRepository.save(analisis));
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        if (!analisisRepository.existsById(id)) {
            throw new NoSuchElementException("Analisis no encontrado: " + id);
        }
        analisisRepository.deleteById(id);
    }

    private void poblar(AnalisisFinanciero analisis, AnalisisFinancieroRequest request) {
        Usuario usuario = analisis.getUsuario();

        // Clasifica las transacciones y agrupa montos por categoria
        Map<Integer, BigDecimal> porCategoria = new LinkedHashMap<>();
        BigDecimal sumaProbabilidades = BigDecimal.ZERO;
        int totalTransacciones = request.transacciones().size();

        for (TransaccionAnalisisRequest tx : request.transacciones()) {
            int idCategoria = clasificacionService.clasificar(tx.descripcion());
            porCategoria.merge(idCategoria, tx.monto(), BigDecimal::add);
            sumaProbabilidades = sumaProbabilidades.add(
                    clasificacionService.clasificarConProbabilidad(tx.descripcion()).getProbabilidad());
        }

        // Clasificacion agregada del analisis
        BigDecimal probabilidad = totalTransacciones == 0
                ? BigDecimal.ZERO
                : sumaProbabilidades.divide(BigDecimal.valueOf(totalTransacciones), 3, RoundingMode.HALF_UP);

        ClasificacionTransaccion clasificacion = new ClasificacionTransaccion();
        clasificacion.setAnalisisFinanciero(analisis);
        clasificacion.setProbabilidad(probabilidad);
        clasificacion.setResumenesGasto(new ArrayList<>());
        analisis.setClasificacionTransaccion(clasificacion);

        // Resumen de gastos por categoria
        for (Map.Entry<Integer, BigDecimal> entry : porCategoria.entrySet()) {
            CategoriaGasto categoria = categoriaGastoRepository.findById(entry.getKey())
                    .orElseThrow(() -> new NoSuchElementException("Categoria no encontrada: " + entry.getKey()));

            ResumenGasto resumen = new ResumenGasto();
            resumen.setClasificacionTransaccion(clasificacion);
            resumen.setCategoriaGasto(categoria);
            resumen.setMontoTotal(entry.getValue());
            clasificacion.getResumenesGasto().add(resumen);
        }

        // Derivar salud financiera segun la probabilidad de clasificacion
        analisis.setSaludFinanciera(derivarSaludFinanciera(probabilidad));

        // Persistir transacciones recibidas por trazabilidad
        persistirTransacciones(usuario, request.transacciones());

        // Generar y persistir recomendaciones
        List<Recomendacion> recomendaciones = generarRecomendaciones(clasificacion, analisis)
                .stream()
                .map(texto -> Recomendacion.builder()
                        .analisisFinanciero(analisis)
                        .descripcion(texto)
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));
        analisis.setRecomendaciones(recomendaciones);
    }

    private SaludFinanciera derivarSaludFinanciera(BigDecimal probabilidad) {
        int id;
        if (probabilidad.compareTo(new BigDecimal("0.800")) >= 0) {
            id = 1; // SALUDABLE
        } else if (probabilidad.compareTo(new BigDecimal("0.650")) >= 0) {
            id = 2; // MODERADA
        } else {
            id = 3; // EN_RIESGO
        }
        return saludFinancieraRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Salud financiera no encontrada: " + id));
    }

    private void persistirTransacciones(Usuario usuario, List<TransaccionAnalisisRequest> transacciones) {
        List<Transaccion> entidades = transacciones.stream()
                .map(tx -> {
                    String nombreTipo = clasificacionService.predecirTipoTransaccion(tx.descripcion()) == 1
                            ? "INGRESO"
                            : "GASTO";
                    TipoTransaccion tipo = tipoTransaccionRepository.findByNombre(nombreTipo)
                            .orElseThrow(() -> new NoSuchElementException(
                                    "Tipo de transaccion " + nombreTipo + " no encontrado"));
                    return Transaccion.builder()
                            .usuario(usuario)
                            .tipoTransaccion(tipo)
                            .descripcion(tx.descripcion())
                            .monto(tx.monto())
                            .fecha(LocalDate.now())
                            .build();
                })
                .toList();
        transaccionRepository.saveAll(entidades);
    }

    private List<String> generarRecomendaciones(ClasificacionTransaccion clasificacion, AnalisisFinanciero analisis) {
        List<String> recomendaciones = new ArrayList<>();

        clasificacion.getResumenesGasto().stream()
                .max(Comparator.comparing(ResumenGasto::getMontoTotal))
                .ifPresent(top -> recomendaciones.add(
                        "Monitorear los gastos recurrentes de " + top.getCategoriaGasto().getNombre()));

        if (clasificacion.getProbabilidad().compareTo(new BigDecimal("0.700")) < 0) {
            recomendaciones.add("Revisar las transacciones clasificadas como 'Otras' para asegurar un registro correcto");
        }

        String salud = analisis.getSaludFinanciera().getNombre();
        switch (salud) {
            case "SALUDABLE" -> recomendaciones.add("Mantener el ritmo actual de ahorro e inversion");
            case "MODERADA" -> {
                recomendaciones.add("Ajustar el presupuesto en las categorias de mayor gasto");
                recomendaciones.add("Destinar al menos un 10% del ingreso al ahorro mensual");
            }
            default -> {
                recomendaciones.add("Reducir los gastos variables y priorizar el pago de deudas");
                recomendaciones.add("Considerar un plan de austeridad de 3 meses para recuperar estabilidad");
            }
        }

        return recomendaciones;
    }

    private AnalisisFinancieroResponse toDetalleResponse(AnalisisFinanciero analisis) {
        List<ResumenGastoResponse> resumenes = new ArrayList<>();
        if (analisis.getClasificacionTransaccion() != null) {
            for (ResumenGasto r : analisis.getClasificacionTransaccion().getResumenesGasto()) {
                resumenes.add(new ResumenGastoResponse(r.getCategoriaGasto().getNombre(), r.getMontoTotal()));
            }
        }

        List<String> recomendaciones = analisis.getRecomendaciones()
                .stream()
                .map(Recomendacion::getDescripcion)
                .toList();

        return new AnalisisFinancieroResponse(
                analisis.getId(),
                analisis.getUsuario().getNombre(),
                analisis.getMes(),
                analisis.getAnio(),
                analisis.getFechaGeneracion(),
                analisis.getSaludFinanciera().getNombre(),
                analisis.getClasificacionTransaccion() == null
                        ? BigDecimal.ZERO
                        : analisis.getClasificacionTransaccion().getProbabilidad(),
                resumenes,
                recomendaciones);
    }
}