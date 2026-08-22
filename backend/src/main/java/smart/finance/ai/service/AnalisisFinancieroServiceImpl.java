package smart.finance.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import smart.finance.ai.config.security.SecurityUtils;
import smart.finance.ai.dto.common.PageResponseDTO;
import smart.finance.ai.dto.ml.MlAnalisisFinancieroRequest;
import smart.finance.ai.dto.ml.MlAnalisisFinancieroResponse;
import smart.finance.ai.dto.request.AnalisisFinancieroRequest;
import smart.finance.ai.dto.request.TransaccionAnalisisRequest;
import smart.finance.ai.dto.response.AnalisisFinancieroResponse;
import smart.finance.ai.dto.response.AnalisisFinancieroResumenResponse;
import smart.finance.ai.dto.response.ResumenGastoResponse;
import smart.finance.ai.dto.response.TransaccionResponseDTO;
import smart.finance.ai.entity.*;
import smart.finance.ai.exception.ForbiddenException;
import smart.finance.ai.exception.ResourceNotFoundException;
import smart.finance.ai.repository.*;
import smart.finance.ai.specification.AnalisisFinancieroSpecification;
import smart.finance.ai.util.CategoriaGastoMapper;
import smart.finance.ai.util.PaginacionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalisisFinancieroServiceImpl implements AnalisisFinancieroService {

    private static final Set<String> CAMPOS_ORDEN = Set.of("id", "mes", "anio", "fechaGeneracion");

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
        Integer usuarioId = SecurityUtils.resolveOwnerOrAdmin(request.usuarioId());

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado: " + usuarioId));

        List<TransaccionAnalisisRequest> transacciones =
                resolverTransaccionesParaCrear(usuario, request.transacciones());
        boolean persistir = request.transacciones() != null && !request.transacciones().isEmpty();

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

        poblar(analisis, request, transacciones, persistir);

        AnalisisFinanciero guardado = analisisRepository.save(analisis);
        return toDetalleResponse(guardado);
    }

    /**
     * Resuelve las transacciones a analizar en modo hibrido (POST):
     * si el body trae lista explicita se usa tal cual (modo simulacion);
     * si es null/vacia se consultan las transacciones persistidas del usuario
     * autenticado en MySQL (modo base de datos).
     */
    private List<TransaccionAnalisisRequest> resolverTransaccionesParaCrear(
            Usuario usuario, List<TransaccionAnalisisRequest> transaccionesBody) {
        if (transaccionesBody != null && !transaccionesBody.isEmpty()) {
            return transaccionesBody;
        }
        List<Transaccion> persistidas = transaccionRepository.findByUsuarioId(usuario.getId());
        if (persistidas.isEmpty()) {
            throw new IllegalArgumentException(
                    "El usuario no tiene transacciones persistidas para generar el análisis.");
        }
        return persistidas.stream()
                .map(tx -> new TransaccionAnalisisRequest(tx.getDescripcion(), tx.getMonto()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<AnalisisFinancieroResumenResponse> historial(Integer usuarioId, String salud,
                                                                        LocalDateTime fechaInicio, LocalDateTime fechaFin,
                                                                        int page, int size, String sort) {
        Integer effectiveUsuarioId;
        if (usuarioId == null) {
            effectiveUsuarioId = SecurityUtils.isAdmin() ? null : SecurityUtils.currentUserId();
        } else {
            effectiveUsuarioId = SecurityUtils.effectiveUserId(usuarioId);
        }

        Specification<AnalisisFinanciero> spec = AnalisisFinancieroSpecification.conFiltros(
                effectiveUsuarioId, salud, fechaInicio, fechaFin);
        Pageable pageable = PaginacionUtils.crearPageable(page, size, sort, CAMPOS_ORDEN, "id");

        Page<AnalisisFinanciero> pagina = analisisRepository.findAll(spec, pageable);
        return new PageResponseDTO<>(pagina.map(this::toResumenResponse));
    }

    private AnalisisFinancieroResumenResponse toResumenResponse(AnalisisFinanciero analisis) {
        return new AnalisisFinancieroResumenResponse(
                analisis.getId(),
                analisis.getUsuario().getId(),
                analisis.getIngresoMensual(),
                analisis.getNivelEndeudamiento(),
                analisis.getFrecuenciaAhorro(),
                analisis.getMes(),
                analisis.getAnio(),
                analisis.getFechaGeneracion(),
                analisis.getSaludFinanciera().getNombre(),
                analisis.getClasificacionTransaccion() == null
                        ? BigDecimal.ZERO
                        : analisis.getClasificacionTransaccion().getProbabilidad());
    }

    @Override
    @Transactional(readOnly = true)
    public AnalisisFinancieroResponse obtener(Integer id) {
        AnalisisFinanciero analisis = obtenerEntidad(id);
        return toDetalleResponse(analisis);
    }

    @Override
    @Transactional
    public AnalisisFinancieroResponse actualizar(Integer id, AnalisisFinancieroRequest request) {
        AnalisisFinanciero analisis = obtenerEntidad(id);

        Integer usuarioId = SecurityUtils.resolveOwnerOrAdmin(request.usuarioId());

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado: " + usuarioId));

        if (request.transacciones() == null || request.transacciones().isEmpty()) {
            throw new IllegalArgumentException(
                    "Debe proporcionar la lista de transacciones al actualizar un análisis financiero.");
        }

        analisis.setUsuario(usuario);
        analisis.setIngresoMensual(request.ingresoMensual());
        analisis.setNivelEndeudamiento(request.nivelEndeudamiento());
        analisis.setFrecuenciaAhorro(request.frecuenciaAhorro());
        analisis.setMes(request.mes());
        analisis.setAnio(request.anio());
        analisis.setFechaGeneracion(LocalDateTime.now());

        analisis.getRecomendaciones().clear();

        poblar(analisis, request, request.transacciones(), true);

        return toDetalleResponse(analisisRepository.save(analisis));
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        analisisRepository.delete(obtenerEntidad(id));
    }

    private AnalisisFinanciero obtenerEntidad(Integer id) {
        if (SecurityUtils.isAdmin()) {
            return analisisRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("El análisis financiero no existe: " + id));
        }
        return analisisRepository.findByIdAndUsuario_Id(id, SecurityUtils.currentUserId())
                .orElseGet(() -> {
                    if (analisisRepository.existsById(id)) {
                        throw new ForbiddenException("El análisis financiero no te pertenece.");
                    }
                    throw new ResourceNotFoundException("El análisis financiero no existe: " + id);
                });
    }

    private void poblar(AnalisisFinanciero analisis, AnalisisFinancieroRequest request,
                        List<TransaccionAnalisisRequest> transacciones, boolean persistir) {
        Usuario usuario = analisis.getUsuario();

        MlAnalisisFinancieroRequest mlRequest = new MlAnalisisFinancieroRequest(
                request.ingresoMensual(),
                request.nivelEndeudamiento(),
                request.frecuenciaAhorro(),
                transacciones);

        mlApiClient.analizarFinanciero(mlRequest)
                .filter(respuesta -> respuesta.getPerfilFinanciero() != null)
                .ifPresentOrElse(
                        respuesta -> poblarDesdeModelo(analisis, respuesta),
                        () -> poblarPorRegla(analisis, request, transacciones));

        // Persistir transacciones recibidas por trazabilidad (solo en modo simulación explícita)
        if (persistir) {
            persistirTransacciones(usuario, transacciones);
        }
    }

    /**
     * Devuelve la {@code ClasificacionTransaccion} existente del análisis para
     * reutilizarla (evitando insertar una fila duplicada que viola la
     * restricción única {@code uq_clasificacion_analisis}) o la crea si no
     * existe. Siempre reinicia la lista de resúmenes de gasto.
     */
    private ClasificacionTransaccion obtenerOCrearClasificacion(AnalisisFinanciero analisis) {
        ClasificacionTransaccion clasificacion = analisis.getClasificacionTransaccion();
        if (clasificacion == null) {
            clasificacion = new ClasificacionTransaccion();
            clasificacion.setAnalisisFinanciero(analisis);
            analisis.setClasificacionTransaccion(clasificacion);
        }
        // Eliminacion explicita con flush: Hibernate inserta los nuevos resumenes
        // ANTES de borrar los viejos, chocando contra uq_resumen_clasificacion_categoria.
        // Borramos y forzamos flush para que el DELETE llegue a BD antes de los INSERT.
        if (clasificacion.getId() != null) {
            List<ResumenGasto> existentes = resumenRepository
                    .findByClasificacionTransaccionId(clasificacion.getId());
            if (!existentes.isEmpty()) {
                resumenRepository.deleteAll(existentes);
                resumenRepository.flush();
            }
        }
        clasificacion.getResumenesGasto().clear();
        return clasificacion;
    }

    /**
     * Rellena la entidad a partir del resultado del modelo de data-science.
     */
    private void poblarDesdeModelo(AnalisisFinanciero analisis, MlAnalisisFinancieroResponse respuesta) {
        BigDecimal probabilidad = respuesta.getProbabilidad() == null
                ? BigDecimal.ZERO
                : respuesta.getProbabilidad().setScale(3, RoundingMode.HALF_UP);

        ClasificacionTransaccion clasificacion = obtenerOCrearClasificacion(analisis);
        clasificacion.setProbabilidad(probabilidad);

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
        analisis.getRecomendaciones().clear();
        analisis.getRecomendaciones().addAll(recomendaciones);
    }

    /**
     * Fallback local cuando el servicio ML no esta disponible: replica la logica
     * de clasificacion por reglas, perfil derivado de la probabilidad y
     * recomendaciones genericas (comportamiento previo a la integracion).
     */
    private void poblarPorRegla(AnalisisFinanciero analisis, AnalisisFinancieroRequest request,
                                List<TransaccionAnalisisRequest> transacciones) {
        Map<Integer, BigDecimal> porCategoria = new LinkedHashMap<>();
        BigDecimal sumaProbabilidades = BigDecimal.ZERO;

        List<String> descripciones = transacciones.stream()
                .map(TransaccionAnalisisRequest::descripcion)
                .toList();
        Map<String, TransaccionResponseDTO> clasificadas =
                clasificacionService.clasificarConProbabilidadLote(descripciones);

        int totalTransacciones = transacciones.size();

        for (TransaccionAnalisisRequest tx : transacciones) {
            TransaccionResponseDTO clasificacion = clasificadas.get(tx.descripcion());
            int idCategoria = clasificacion.getIdCategoria();
            porCategoria.merge(idCategoria, tx.monto(), BigDecimal::add);
            sumaProbabilidades = sumaProbabilidades.add(clasificacion.getProbabilidad());
        }

        BigDecimal probabilidad = totalTransacciones == 0
                ? BigDecimal.ZERO
                : sumaProbabilidades.divide(BigDecimal.valueOf(totalTransacciones), 3, RoundingMode.HALF_UP);

        ClasificacionTransaccion clasificacion = obtenerOCrearClasificacion(analisis);
        clasificacion.setProbabilidad(probabilidad);

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
        analisis.getRecomendaciones().clear();
        analisis.getRecomendaciones().addAll(recomendaciones);
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
                analisis.getUsuario().getId(),
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