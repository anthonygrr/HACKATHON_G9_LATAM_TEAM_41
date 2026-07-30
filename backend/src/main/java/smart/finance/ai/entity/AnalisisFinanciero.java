package smart.finance.ai.entity;

import jakarta.persistence.*;
import lombok.*;
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
    @Column(name = "id")
    private Long id;

    @Column(name = "mes", nullable = false)
    private Integer mes;

    @Column(name = "ano", nullable = false)
    private Integer ano;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk id_salud_financiera", nullable = false)
    private SaludFinanciera saludFinanciera;

    @Column(name = "fecha_generacion", nullable = false)
    private LocalDateTime fechaGeneracion;

    @OneToOne(mappedBy = "analisisFinanciero", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ClasificacionTransaccion clasificacionTransaccion;

    @OneToMany(mappedBy = "analisisFinanciero", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Recomendacion> recomendaciones = new ArrayList<>();
}