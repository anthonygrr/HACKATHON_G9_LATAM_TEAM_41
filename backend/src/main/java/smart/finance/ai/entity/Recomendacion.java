package smart.finance.ai.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "recomendacion")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Recomendacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_recomendacion")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_analisis_financiero", nullable = false)
    private AnalisisFinanciero analisisFinanciero;

    @Column(name = "descripcion", nullable = false, length = 500)
    private String descripcion;
}