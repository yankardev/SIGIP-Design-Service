package pe.edu.cibertec.sigip.designservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Versión de diseño de un pedido. Las versiones anteriores no se borran: quedan como historial. */
@Entity
@Table(name = "disenos",
        uniqueConstraints = @UniqueConstraint(name = "uk_diseno_pedido_version", columnNames = {"idpedido", "version"}),
        indexes = @Index(name = "idx_diseno_cliente", columnList = "cliente"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Diseno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer iddiseno;

    @Column(nullable = false)
    private Integer idpedido;

    /** Dueño del pedido (obtenido de sales-service), único que puede aprobar u observar. */
    @Column(nullable = false, length = 50)
    private String cliente;

    @Column(nullable = false)
    private Integer version;

    @Column(nullable = false, length = 200)
    private String nombreArchivo;

    /** Enlace al archivo (el binario se guarda fuera de la BD y de Git). */
    @Column(length = 500)
    private String urlArchivo;

    @Column(length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoDiseno estado;

    @Column(length = 500)
    private String comentarioCliente;

    @Column(nullable = false, length = 50)
    private String registradoPor;

    @Column(nullable = false)
    private LocalDateTime fechaRegistro;

    private LocalDateTime fechaDecision;
}
