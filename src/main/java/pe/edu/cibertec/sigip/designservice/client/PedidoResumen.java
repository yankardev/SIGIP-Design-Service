package pe.edu.cibertec.sigip.designservice.client;

/** Datos mínimos del pedido que necesita design-service (contrato con sales-service). */
public record PedidoResumen(Integer idpedido, String cliente, String estado) {
}
