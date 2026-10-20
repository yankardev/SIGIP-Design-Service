package pe.edu.cibertec.sigip.designservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DecisionRequest(
        @NotNull(message = "La decisión es obligatoria (APROBAR u OBSERVAR)")
        Decision decision,

        @Size(max = 500, message = "El comentario no puede superar 500 caracteres")
        String comentario
) {
    public enum Decision { APROBAR, OBSERVAR }
}
