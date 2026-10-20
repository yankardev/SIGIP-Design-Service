package pe.edu.cibertec.sigip.designservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.edu.cibertec.sigip.designservice.model.Diseno;

import java.util.List;
import java.util.Optional;

public interface DisenoRepository extends JpaRepository<Diseno, Integer> {

    List<Diseno> findByIdpedidoOrderByVersionDesc(Integer idpedido);

    List<Diseno> findByClienteOrderByFechaRegistroDesc(String cliente);

    List<Diseno> findAllByOrderByFechaRegistroDesc();

    Optional<Diseno> findFirstByIdpedidoOrderByVersionDesc(Integer idpedido);

    @Query("select coalesce(max(d.version), 0) from Diseno d where d.idpedido = :idpedido")
    int ultimaVersion(Integer idpedido);
}
