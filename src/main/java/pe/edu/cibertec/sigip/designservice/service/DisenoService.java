package pe.edu.cibertec.sigip.designservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.cibertec.sigip.common.error.RecursoNoEncontradoException;
import pe.edu.cibertec.sigip.common.error.ReglaNegocioException;
import pe.edu.cibertec.sigip.common.security.UsuarioActual;
import pe.edu.cibertec.sigip.designservice.client.CambioEstadoPedido;
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
 * Reglas de diseño:
 * - El pedido debe tener el pago verificado antes de entrar al área de diseño.
 * - La primera propuesta mueve el pedido de CONFIRMADO a EN_DISENO.
 * - Cada nueva versión reemplaza a la anterior y conserva el historial.
 * - El cliente dueño del pedido aprueba u observa la versión vigente.
 */
@Service
@RequiredArgsConstructor
public class DisenoService {

    private static final Set<String> ESTADOS_PEDIDO_VALIDOS = Set.of("CONFIRMADO", "EN_DISENO");

    private final DisenoRepository disenoRepository;
    private final SalesServiceClient salesClient;

    @Transactional(readOnly = true)
    public List<DisenoResponse> listar(Integer idpedido, UsuarioActual usuario) {
        List<Diseno> disenos;
        if (idpedido != null) {
            disenos = disenoRepository.findByIdpedidoOrderByVersionDesc(idpedido);
        } else if (usuario.esInterno()) {
            disenos = disenoRepository.findAllByOrderByFechaRegistroDesc();
        } else {
            disenos = disenoRepository.findByClienteOrderByFechaRegistroDesc(usuario.nombre());
        }
        return disenos.stream()
                .filter(d -> usuario.esInterno() || d.getCliente().equals(usuario.nombre()))
                .map(DisenoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DisenoResponse obtener(Integer id, UsuarioActual usuario) {
        return DisenoResponse.from(buscarVisible(id, usuario));
    }

    @Transactional
    public DisenoResponse registrar(DisenoRequest request, UsuarioActual usuario) {
        PedidoResumen pedido = salesClient.obtenerPedido(request.idpedido());
        if (!ESTADOS_PEDIDO_VALIDOS.contains(pedido.estado())) {
            throw new ReglaNegocioException("El pedido debe estar CONFIRMADO para iniciar diseño (estado actual: "
                    + pedido.estado() + ")");
        }

        disenoRepository.findFirstByIdpedidoOrderByVersionDesc(request.idpedido()).ifPresent(anterior -> {
            if (anterior.getEstado() == EstadoDiseno.APROBADO) {
                throw new ReglaNegocioException("El pedido ya tiene un diseño aprobado (versión " + anterior.getVersion() + ")");
            }
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
                    new CambioEstadoPedido("EN_DISENO", "Diseño enviado al cliente para revisión"));
        }
        return DisenoResponse.from(guardado);
    }

    @Transactional
    public DisenoResponse actualizar(Integer id, DisenoRequest request) {
        Diseno diseno = buscar(id);
        exigirEnRevision(diseno, "Solo se puede editar un diseño en revisión");
        if (!diseno.getIdpedido().equals(request.idpedido())) {
            throw new ReglaNegocioException("No se puede cambiar el pedido de un diseño; registre una nueva versión");
        }
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
        Diseno diseno = buscarVisible(id, usuario);
        exigirEnRevision(diseno, "Esta versión ya no está en revisión (estado " + diseno.getEstado() + ")");

        boolean aprobar = request.decision() == DecisionRequest.Decision.APROBAR;
        if (!aprobar && (request.comentario() == null || request.comentario().isBlank())) {
            throw new ReglaNegocioException("Indique qué cambios necesita en el comentario");
        }
        diseno.setEstado(aprobar ? EstadoDiseno.APROBADO : EstadoDiseno.OBSERVADO);
        diseno.setComentarioCliente(request.comentario());
        diseno.setFechaDecision(LocalDateTime.now());
        return DisenoResponse.from(disenoRepository.save(diseno));
    }

    private Diseno buscar(Integer id) {
        return disenoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Diseño no encontrado"));
    }

    private Diseno buscarVisible(Integer id, UsuarioActual usuario) {
        Diseno diseno = buscar(id);
        if (!usuario.esInterno() && !diseno.getCliente().equals(usuario.nombre())) {
            throw new RecursoNoEncontradoException("Diseño no encontrado");
        }
        return diseno;
    }

    private void exigirEnRevision(Diseno diseno, String mensaje) {
        if (diseno.getEstado() != EstadoDiseno.EN_REVISION) {
            throw new ReglaNegocioException(mensaje);
        }
    }
}
