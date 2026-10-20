package pe.edu.cibertec.sigip.designservice.dto;

import pe.edu.cibertec.sigip.designservice.model.Diseno;
import pe.edu.cibertec.sigip.designservice.model.EstadoDiseno;

import java.time.LocalDateTime;

public record DisenoResponse(
        Integer iddiseno,
        Integer idpedido,
        String cliente,
        Integer version,
        String nombreArchivo,
        String urlArchivo,
        String descripcion,
        EstadoDiseno estado,
        String comentarioCliente,
        String registradoPor,
        LocalDateTime fechaRegistro,
        LocalDateTime fechaDecision
) {
    public static DisenoResponse from(Diseno d) {
        return new DisenoResponse(d.getIddiseno(), d.getIdpedido(), d.getCliente(), d.getVersion(),
                d.getNombreArchivo(), d.getUrlArchivo(), d.getDescripcion(), d.getEstado(),
                d.getComentarioCliente(), d.getRegistradoPor(), d.getFechaRegistro(), d.getFechaDecision());
    }
}
