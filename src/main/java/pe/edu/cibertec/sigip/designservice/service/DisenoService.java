package pe.edu.cibertec.sigip.designservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.cibertec.sigip.common.error.RecursoNoEncontradoException;
import pe.edu.cibertec.sigip.common.error.ReglaNegocioException;
import pe.edu.cibertec.sigip.common.security.UsuarioActual;
import pe.edu.cibertec.sigip.designservice.client.PedidoResumen;
import pe.edu.cibertec.sigip.designservice.client.SalesServiceClient;
import pe.edu.cibertec.sigip.designservice.dto.DecisionRequest;
import pe.edu.cibertec.sigip.designservice.dto.DisenoRequest;
import pe.edu.cibertec.sigip.designservice.dto.DisenoResponse;
import pe.edu.cibertec.sigip.designservice.model.Diseno;
import pe.edu.cibertec.sigip.designservice.model.EstadoDiseno;
import pe.edu.cibertec.sigip.designservice.repository.DisenoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Gestión interna del arte: Operaciones publica versiones y valida la versión final
 * antes de permitir que el trabajo continúe a producción.
 */
@Service
@RequiredArgsConstructor
public class DisenoService {

    private static final Set<String> ESTADOS_PEDIDO_VALIDOS = Set.of("CONFIRMADO", "EN_DISENO");

    private final DisenoRepository disenoRepository;
    private final SalesServiceClient salesClient;

    @Transactional(readOnly = true)
    public List<DisenoResponse> listar(Integer idpedido, UsuarioActual usuario) {
        List<Diseno> disenos = idpedido != null
                ? disenoRepository.findByIdpedidoOrderByVersionDesc(idpedido)
                : disenoRepository.findAllByOrderByFechaRegistroDesc();
        return disenos.stream().map(DisenoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public DisenoResponse obtener(Integer id, UsuarioActual usuario) {
        return DisenoResponse.from(buscar(id));
    }

    @Transactional
    public DisenoResponse registrar(DisenoRequest request, UsuarioActual usuario) {
        PedidoResumen pedido = salesClient.obtenerPedido(request.idpedido());
        if (!ESTADOS_PEDIDO_VALIDOS.contains(pedido.estado())) {
            throw new ReglaNegocioException("El diseño solo puede iniciarse después de verificar el pago. Estado actual: " + pedido.estado());
        }

        disenoRepository.findFirstByIdpedidoOrderByVersionDesc(request.idpedido()).ifPresent(anterior -> {
            if (anterior.getEstado() == EstadoDiseno.APROBADO)
                throw new ReglaNegocioException("El pedido ya tiene un diseño aprobado (versión " + anterior.getVersion() + ")");
            anterior.setEstado(EstadoDiseno.REEMPLAZADO);
        });

        Diseno diseno = Diseno.builder()
                .idpedido(request.idpedido())
                .cliente(pedido.cliente())
                .version(disenoRepository.ultimaVersion(request.idpedido()) + 1)
                .nombreArchivo(request.nombreArchivo())
                .urlArchivo(request.urlArchivo())
                .descripcion(request.descripcion())
                .estado(EstadoDiseno.EN_REVISION)
                .registradoPor(usuario.nombre())
                .fechaRegistro(LocalDateTime.now())
                .build();

        Diseno guardado = disenoRepository.save(diseno);
        if ("CONFIRMADO".equals(pedido.estado())) {
            salesClient.cambiarEstado(request.idpedido(),
                    new SalesServiceClient.CambioEstado("EN_DISENO", "Diseño iniciado por " + usuario.nombre()));
        }
        return DisenoResponse.from(guardado);
    }

    @Transactional
    public DisenoResponse actualizar(Integer id, DisenoRequest request) {
        Diseno diseno = buscar(id);
        exigirEnRevision(diseno, "Solo se puede editar un diseño en revisión");
        if (!diseno.getIdpedido().equals(request.idpedido()))
            throw new ReglaNegocioException("No se puede cambiar el pedido de un diseño; registre una nueva versión");
        diseno.setNombreArchivo(request.nombreArchivo());
        diseno.setUrlArchivo(request.urlArchivo());
        diseno.setDescripcion(request.descripcion());
        return DisenoResponse.from(disenoRepository.save(diseno));
    }

    @Transactional
    public void eliminar(Integer id) {
        Diseno diseno = buscar(id);
        exigirEnRevision(diseno, "Solo se puede eliminar un diseño en revisión");
        disenoRepository.delete(diseno);
    }

    @Transactional
    public DisenoResponse decidir(Integer id, DecisionRequest request, UsuarioActual usuario) {
        Diseno diseno = buscar(id);
        exigirEnRevision(diseno, "Esta versión ya no está en revisión (estado " + diseno.getEstado() + ")");
        boolean aprobar = request.decision() == DecisionRequest.Decision.APROBAR;
        if (!aprobar && (request.comentario() == null || request.comentario().isBlank()))
            throw new ReglaNegocioException("Indique la observación o cambio requerido");
        diseno.setEstado(aprobar ? EstadoDiseno.APROBADO : EstadoDiseno.OBSERVADO);
        String comentario = request.comentario();
        diseno.setComentarioCliente((comentario == null || comentario.isBlank())
                ? "Validado internamente por " + usuario.nombre()
                : comentario + " · validado por " + usuario.nombre());
        diseno.setFechaDecision(LocalDateTime.now());
        return DisenoResponse.from(disenoRepository.save(diseno));
    }

    private Diseno buscar(Integer id) {
        return disenoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Diseño no encontrado"));
    }

    private void exigirEnRevision(Diseno diseno, String mensaje) {
        if (diseno.getEstado() != EstadoDiseno.EN_REVISION) throw new ReglaNegocioException(mensaje);
    }
}
