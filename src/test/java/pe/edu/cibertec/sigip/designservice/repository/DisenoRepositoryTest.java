package pe.edu.cibertec.sigip.designservice.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import pe.edu.cibertec.sigip.designservice.model.Diseno;
import pe.edu.cibertec.sigip.designservice.model.EstadoDiseno;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** Pruebas de la capa de acceso a datos de diseños: insertar, actualizar, eliminar y listar. */
@DataJpaTest
@ActiveProfiles("test")
class DisenoRepositoryTest {

    @Autowired
    private DisenoRepository repository;

    @Test
    void debeInsertarDiseno() {
        assertThat(repository.save(diseno(1, 1)).getIddiseno()).isNotNull();
    }

    @Test
    void debeActualizarDiseno() {
        Diseno guardado = repository.saveAndFlush(diseno(1, 1));
        guardado.setEstado(EstadoDiseno.APROBADO);
        repository.saveAndFlush(guardado);

        assertThat(repository.findById(guardado.getIddiseno())).get()
                .extracting(Diseno::getEstado).isEqualTo(EstadoDiseno.APROBADO);
    }

    @Test
    void debeEliminarDiseno() {
        Diseno guardado = repository.saveAndFlush(diseno(1, 1));
        repository.deleteById(guardado.getIddiseno());
        repository.flush();

        assertThat(repository.findById(guardado.getIddiseno())).isEmpty();
    }

    @Test
    void debeListarVersionesDelPedido() {
        repository.save(diseno(7, 1));
        repository.save(diseno(7, 2));
        repository.save(diseno(8, 1));

        assertThat(repository.findAll()).hasSize(3);
        assertThat(repository.findByIdpedidoOrderByVersionDesc(7)).extracting(Diseno::getVersion).containsExactly(2, 1);
        assertThat(repository.ultimaVersion(7)).isEqualTo(2);
        assertThat(repository.ultimaVersion(99)).isZero();
    }

    private Diseno diseno(int idpedido, int version) {
        return Diseno.builder().idpedido(idpedido).cliente("cliente01").version(version)
                .nombreArchivo("logo-v" + version + ".pdf").estado(EstadoDiseno.EN_REVISION)
                .registradoPor("admin").fechaRegistro(LocalDateTime.now()).build();
    }
}
