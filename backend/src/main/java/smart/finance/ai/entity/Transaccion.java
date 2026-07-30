package smart.finance.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transaccion")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Transaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "monto", nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk id_tipo_transaccion", nullable = false)
    private TipoTransaccion tipoTransaccion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk id_usuario", nullable = false)
    private Usuario usuario;
}