package smart.finance.ai.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "salud_financiera")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SaludFinanciera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_salud_financiera")
    private Integer id;

    @Column(name = "nombre", nullable = false, unique = true, length = 50)
    private String nombre;
}