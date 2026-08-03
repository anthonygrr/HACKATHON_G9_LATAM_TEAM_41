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
    @Column(name = "id_resumen_gasto")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_clasificacion_transaccion", nullable = false)
    private ClasificacionTransaccion clasificacionTransaccion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_categoria_gasto", nullable = false)
    private CategoriaGasto categoriaGasto;

    @Column(name = "monto_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoTotal;
}