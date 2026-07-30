package smart.finance.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "resumen_gasto")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ResumenGasto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk id_categoria_gasto", nullable = false)
    private CategoriaGasto categoriaGasto;

    @Column(name = "monto", nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk id_clasificacion_transaccion", nullable = false)
    private ClasificacionTransaccion clasificacionTransaccion;
}
