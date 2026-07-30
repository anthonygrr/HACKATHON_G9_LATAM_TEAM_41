package smart.finance.ai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "USUARIO")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "apellido paterno", nullable = false, length = 100)
    private String apellidoPaterno;

    @Column(name = "apellido materno", length = 100)
    private String apellidoMaterno;

    @Column(name = "Fecha de nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "Correo", nullable = false, unique = true, length = 150)
    private String correo;

    @Column(name = "contraseña", nullable = false, length = 255)
    private String contrasena;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 20)
    @Builder.Default
    private Rol rol = Rol.ROLE_USER;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Transaccion> transacciones = new ArrayList<>();

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AnalisisFinanciero> analisisFinancieros = new ArrayList<>();
}
