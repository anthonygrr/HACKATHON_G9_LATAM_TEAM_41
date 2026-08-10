package smart.finance.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "analisis_financiero")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AnalisisFinanciero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_analisis_financiero")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_salud_financiera", nullable = false)
    private SaludFinanciera saludFinanciera;

    @Column(name = "mes", nullable = false)
    private Integer mes;

    @Column(name = "anio", nullable = false)
    private Integer anio;

    @Column(name = "fecha_generacion", nullable = false)
    private LocalDateTime fechaGeneracion;

    @Column(name = "ingreso_mensual", nullable = false)
    private BigDecimal ingresoMensual;

    @Column(name = "nivel_endeudamiento", nullable = false)
    private BigDecimal nivelEndeudamiento;

    @Column(name = "frecuencia_ahorro", nullable = false)
    private String frecuenciaAhorro;

    @OneToOne(mappedBy = "analisisFinanciero", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ClasificacionTransaccion clasificacionTransaccion;

    @OneToMany(mappedBy = "analisisFinanciero", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Recomendacion> recomendaciones = new ArrayList<>();
}