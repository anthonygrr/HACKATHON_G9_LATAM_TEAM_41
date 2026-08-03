package smart.finance.ai.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categoria_gasto")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CategoriaGasto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria_gasto")
    private Integer id;

    @Column(name = "nombre", nullable = false, unique = true, length = 100)
    private String nombre;
}