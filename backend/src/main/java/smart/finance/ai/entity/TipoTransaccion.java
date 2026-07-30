package smart.finance.ai.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tipo_transaccion")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TipoTransaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_tipo_transaccion")
    private Long idTipoTransaccion;

    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;
}