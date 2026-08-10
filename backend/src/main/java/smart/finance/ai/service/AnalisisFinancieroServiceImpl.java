package smart.finance.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import smart.finance.ai.dto.ml.MlAnalisisFinancieroRequest;
import smart.finance.ai.dto.ml.MlAnalisisFinancieroResponse;
import smart.finance.ai.dto.request.AnalisisFinancieroRequest;
import smart.finance.ai.dto.request.TransaccionAnalisisRequest;
import smart.finance.ai.dto.response.AnalisisFinancieroResponse;
import smart.finance.ai.dto.response.AnalisisFinancieroResumenResponse;
import smart.finance.ai.dto.response.ResumenGastoResponse;
import smart.finance.ai.dto.response.TransaccionResponseDTO;
import smart.finance.ai.entity.*;
import smart.finance.ai.repository.*;
import smart.finance.ai.util.CategoriaGastoMapper;

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
    private final MlApiClient mlApiClient;

    @Override
    @Transactional
    public AnalisisFinancieroResponse crear(AnalisisFinancieroRequest request) {
        Usuario usuario = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado: " + request.usuarioId()));

        AnalisisFinanciero analisis = AnalisisFinanciero.builder()
                .usuario(usuario)
                .ingresoMensual(request.ingresoMensual())
                .nivelEndeudamiento(request.nivelEndeudamiento())
                .frecuenciaAhorro(request.frecuenciaAhorro())
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
                        a.getIngresoMensual(),
                        a.getNivelEndeudamiento(),
                        a.getFrecuenciaAhorro(),
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
        analisis.setIngresoMensual(request.ingresoMensual());
        analisis.setNivelEndeudamiento(request.nivelEndeudamiento());
        analisis.setFrecuenciaAhorro(request.frecuenciaAhorro());
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

        MlAnalisisFinancieroRequest mlRequest = new MlAnalisisFinancieroRequest(
                request.ingresoMensual(),
                request.nivelEndeudamiento(),
                request.frecuenciaAhorro(),
                request.transacciones());

        mlApiClient.analizarFinanciero(mlRequest)
                .filter(respuesta -> respuesta.getPerfilFinanciero() != null)
                .ifPresentOrElse(
                        respuesta -> poblarDesdeModelo(analisis, respuesta),
                        () -> poblarPorRegla(analisis, request));

        // Persistir transacciones recibidas por trazabilidad
        persistirTransacciones(usuario, request.transacciones());
    }

    /**
     * Rellena la entidad a partir del resultado del modelo de data-science.
     */
    private void poblarDesdeModelo(AnalisisFinanciero analisis, MlAnalisisFinancieroResponse respuesta) {
        BigDecimal probabilidad = respuesta.getProbabilidad() == null
                ? BigDecimal.ZERO
                : respuesta.getProbabilidad().setScale(3, RoundingMode.HALF_UP);

        ClasificacionTransaccion clasificacion = new ClasificacionTransaccion();
        clasificacion.setAnalisisFinanciero(analisis);
        clasificacion.setProbabilidad(probabilidad);
        clasificacion.setResumenesGasto(new ArrayList<>());
        analisis.setClasificacionTransaccion(clasificacion);

        if (respuesta.getResumenGastos() != null) {
            respuesta.getResumenGastos().forEach((slug, monto) -> {
                Integer idCategoria = CategoriaGastoMapper.idDesdeSlug(slug);
                if (idCategoria == null) {
                    return;
                }
                CategoriaGasto categoria = categoriaGastoRepository.findById(idCategoria)
                        .orElseThrow(() -> new NoSuchElementException("Categoria no encontrada: " + idCategoria));
                ResumenGasto resumen = new ResumenGasto();
                resumen.setClasificacionTransaccion(clasificacion);
                resumen.setCategoriaGasto(categoria);
                resumen.setMontoTotal(monto == null ? BigDecimal.ZERO : monto);
                clasificacion.getResumenesGasto().add(resumen);
            });
        }

        analisis.setSaludFinanciera(derivarSaludFinancieraDesdePerfil(respuesta.getPerfilFinanciero()));

        List<Recomendacion> recomendaciones = (respuesta.getRecomendaciones() == null
                ? new ArrayList<String>()
                : respuesta.getRecomendaciones())
                .stream()
                .map(texto -> Recomendacion.builder()
                        .analisisFinanciero(analisis)
                        .descripcion(texto)
                        .build())
                .toList();
        analisis.setRecomendaciones(recomendaciones);
    }

    /**
     * Fallback local cuando el servicio ML no esta disponible: replica la logica
     * de clasificacion por reglas, perfil derivado de la probabilidad y
     * recomendaciones genericas (comportamiento previo a la integracion).
     */
    private void poblarPorRegla(AnalisisFinanciero analisis, AnalisisFinancieroRequest request) {
        Map<Integer, BigDecimal> porCategoria = new LinkedHashMap<>();
        BigDecimal sumaProbabilidades = BigDecimal.ZERO;

        List<String> descripciones = request.transacciones().stream()
                .map(TransaccionAnalisisRequest::descripcion)
                .toList();
        Map<String, TransaccionResponseDTO> clasificadas =
                clasificacionService.clasificarConProbabilidadLote(descripciones);

        int totalTransacciones = request.transacciones().size();

        for (TransaccionAnalisisRequest tx : request.transacciones()) {
            TransaccionResponseDTO clasificacion = clasificadas.get(tx.descripcion());
            int idCategoria = clasificacion.getIdCategoria();
            porCategoria.merge(idCategoria, tx.monto(), BigDecimal::add);
            sumaProbabilidades = sumaProbabilidades.add(clasificacion.getProbabilidad());
        }

        BigDecimal probabilidad = totalTransacciones == 0
                ? BigDecimal.ZERO
                : sumaProbabilidades.divide(BigDecimal.valueOf(totalTransacciones), 3, RoundingMode.HALF_UP);

        ClasificacionTransaccion clasificacion = new ClasificacionTransaccion();
        clasificacion.setAnalisisFinanciero(analisis);
        clasificacion.setProbabilidad(probabilidad);
        clasificacion.setResumenesGasto(new ArrayList<>());
        analisis.setClasificacionTransaccion(clasificacion);

        for (Map.Entry<Integer, BigDecimal> entry : porCategoria.entrySet()) {
            CategoriaGasto categoria = categoriaGastoRepository.findById(entry.getKey())
                    .orElseThrow(() -> new NoSuchElementException("Categoria no encontrada: " + entry.getKey()));

            ResumenGasto resumen = new ResumenGasto();
            resumen.setClasificacionTransaccion(clasificacion);
            resumen.setCategoriaGasto(categoria);
            resumen.setMontoTotal(entry.getValue());
            clasificacion.getResumenesGasto().add(resumen);
        }

        analisis.setSaludFinanciera(derivarSaludFinanciera(probabilidad));

        List<Recomendacion> recomendaciones = generarRecomendaciones(clasificacion, analisis)
                .stream()
                .map(texto -> Recomendacion.builder()
                        .analisisFinanciero(analisis)
                        .descripcion(texto)
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));
        analisis.setRecomendaciones(recomendaciones);
    }

    /**
     * Mapea el perfil predicho por el modelo de data-science a la entidad
     * {@code salud_financiera}. Si el perfil no es reconocido, degrada al
     * criterio por probabilidad.
     */
    private SaludFinanciera derivarSaludFinancieraDesdePerfil(String perfilFinanciero) {
        Integer id = CategoriaGastoMapper.idSaludFinanciera(perfilFinanciero);
        if (id == null) {
            return derivarSaludFinanciera(BigDecimal.ZERO);
        }
        return saludFinancieraRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Salud financiera no encontrada: " + id));
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
                analisis.getIngresoMensual(),
                analisis.getNivelEndeudamiento(),
                analisis.getFrecuenciaAhorro(),
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