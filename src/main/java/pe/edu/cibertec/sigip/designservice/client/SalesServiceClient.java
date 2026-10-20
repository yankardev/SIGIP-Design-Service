package pe.edu.cibertec.sigip.designservice.client;

import feign.FeignException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import pe.edu.cibertec.sigip.common.error.RecursoNoEncontradoException;
import pe.edu.cibertec.sigip.common.error.ServicioNoDisponibleException;

/** Consulta a sales-service para validar el pedido y conocer a su dueño (el token del usuario se propaga). */
@FeignClient(name = "sales-service", fallbackFactory = SalesServiceClient.Fallback.class)
public interface SalesServiceClient {

    @GetMapping("/api/v1/pedidos/{id}")
    PedidoResumen obtenerPedido(@PathVariable("id") Integer id);

    @Component
    class Fallback implements FallbackFactory<SalesServiceClient> {
        @Override
        public SalesServiceClient create(Throwable causa) {
            return id -> {
                if (causa instanceof FeignException.NotFound) {
                    throw new RecursoNoEncontradoException("El pedido " + id + " no existe");
                }
                throw new ServicioNoDisponibleException("El servicio de ventas no está disponible en este momento");
            };
        }
    }
}
