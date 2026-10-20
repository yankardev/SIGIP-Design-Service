package pe.edu.cibertec.sigip.designservice.model;

public enum EstadoDiseno {
    /** Publicado y esperando la respuesta del cliente. */
    EN_REVISION,
    APROBADO,
    /** El cliente pidió cambios: se espera una nueva versión. */
    OBSERVADO,
    /** Una versión más reciente la sustituyó; se conserva como historial. */
    REEMPLAZADO
}
