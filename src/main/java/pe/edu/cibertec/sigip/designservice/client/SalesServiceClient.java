package pe.edu.cibertec.sigip.designservice.client;

import feign.FeignException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import pe.edu.cibertec.sigip.common.error.RecursoNoEncontradoException;
import pe.edu.cibertec.sigip.common.error.ServicioNoDisponibleException;

/** Consulta y actualiza el pedido en sales-service; el token del usuario se propaga. */
@FeignClient(name = "sales-service", fallbackFactory = SalesServiceClient.Fallback.class)
public interface SalesServiceClient {

    @GetMapping("/api/v1/pedidos/{id}")
    PedidoResumen obtenerPedido(@PathVariable("id") Integer id);

    @PatchMapping("/api/v1/pedidos/{id}/estado")
    ResponseEntity<Void> cambiarEstado(@PathVariable("id") Integer id, @RequestBody CambioEstadoPedido request);

    @Component
    class Fallback implements FallbackFactory<SalesServiceClient> {
        @Override
        public SalesServiceClient create(Throwable causa) {
            return new SalesServiceClient() {
                @Override
                public PedidoResumen obtenerPedido(Integer id) {
                    if (causa instanceof FeignException.NotFound) {
                        throw new RecursoNoEncontradoException("El pedido " + id + " no existe");
                    }
                    throw new ServicioNoDisponibleException("El servicio de ventas no está disponible en este momento");
                }

                @Override
                public ResponseEntity<Void> cambiarEstado(Integer id, CambioEstadoPedido request) {
                    throw new ServicioNoDisponibleException("No se pudo actualizar el estado del pedido en ventas");
                }
            };
        }
    }
}
