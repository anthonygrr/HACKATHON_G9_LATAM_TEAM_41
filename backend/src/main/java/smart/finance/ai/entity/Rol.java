package smart.finance.ai.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rol")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Rol {

    @Id
    @Column(name = "id_rol")
    private Integer id;

    @Column(name = "nombre", nullable = false, unique = true, length = 20)
    private String nombre;
}