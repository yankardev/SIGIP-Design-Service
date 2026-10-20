package pe.edu.cibertec.sigip.designservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DisenoRequest(
        @NotNull(message = "El pedido es obligatorio")
        Integer idpedido,

        @NotBlank(message = "El nombre del archivo es obligatorio")
        @Size(max = 200, message = "El nombre del archivo no puede superar 200 caracteres")
        @Pattern(regexp = "(?i)^[\\w\\-. ]+\\.(pdf|png|jpg|jpeg|ai|psd|svg|cdr)$",
                message = "Formato permitido: pdf, png, jpg, jpeg, ai, psd, svg o cdr")
        String nombreArchivo,

        @Size(max = 500, message = "La URL no puede superar 500 caracteres")
        @Pattern(regexp = "^$|^https?://.+", message = "La URL debe empezar con http:// o https://")
        String urlArchivo,

        @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
        String descripcion
) {
}
