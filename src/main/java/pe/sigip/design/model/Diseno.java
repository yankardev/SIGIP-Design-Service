package pe.sigip.design.model;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
@Entity @Table(name="disenos")
public class Diseno {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long pedidoId;
 @NotBlank @Column(nullable=false,length=180) private String nombre;
 @Column(length=1000) private String urlArchivo;
 @Column(nullable=false,length=30) private String estado="PENDIENTE";
 @Column(nullable=false) private Integer version=1;
 @Column(nullable=false) private LocalDateTime fechaCreacion=LocalDateTime.now();
 public Long getId(){return id;} public Long getPedidoId(){return pedidoId;} public String getNombre(){return nombre;} public String getUrlArchivo(){return urlArchivo;} public String getEstado(){return estado;} public Integer getVersion(){return version;} public LocalDateTime getFechaCreacion(){return fechaCreacion;}
 public void setId(Long v){id=v;} public void setPedidoId(Long v){pedidoId=v;} public void setNombre(String v){nombre=v;} public void setUrlArchivo(String v){urlArchivo=v;} public void setEstado(String v){estado=v;} public void setVersion(Integer v){version=v;} public void setFechaCreacion(LocalDateTime v){fechaCreacion=v;}
}