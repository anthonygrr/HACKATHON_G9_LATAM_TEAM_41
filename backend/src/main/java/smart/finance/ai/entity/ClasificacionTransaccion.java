package smart.finance.ai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clasificacion_transaccion")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ClasificacionTransaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_clasificacion_transaccion")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_analisis_financiero", nullable = false, unique = true)
    private AnalisisFinanciero analisisFinanciero;

    @Column(name = "probabilidad", nullable = false, precision = 4, scale = 3)
    private BigDecimal probabilidad;

    @OneToMany(mappedBy = "clasificacionTransaccion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ResumenGasto> resumenesGasto = new ArrayList<>();
}