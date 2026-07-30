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
    @Column(name = "id")
    private Long id;

    @Column(name = "probabilidad", nullable = false, precision = 5, scale = 4)
    private BigDecimal probabilidad;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk id_analisis_financiero", nullable = false, unique = true)
    private AnalisisFinanciero analisisFinanciero;

    @OneToMany(mappedBy = "clasificacionTransaccion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ResumenGasto> resumenesGasto = new ArrayList<>();
}