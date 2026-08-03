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
    @Column(name = "id_tipo_transaccion")
    private Integer id;

    @Column(name = "nombre", nullable = false, unique = true, length = 20)
    private String nombre;
}