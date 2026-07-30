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
    @Column(name = "id")
    private Long id;

    @Column(name = "tipo", nullable = false, length = 50)
    private String tipo;
}