package pe.edu.cibertec.sigip.designservice.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.cibertec.sigip.common.security.JwtService;
import pe.edu.cibertec.sigip.designservice.client.PedidoResumen;
import pe.edu.cibertec.sigip.designservice.client.SalesServiceClient;
import pe.edu.cibertec.sigip.designservice.model.Diseno;
import pe.edu.cibertec.sigip.designservice.repository.DisenoRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Versiones de diseño y aprobación del cliente, de punta a punta por REST. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DisenoFlujoIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private DisenoRepository repository;
    @MockitoBean
    private SalesServiceClient salesClient;

    private String tokenAdmin;
    private String tokenCliente;
    private String tokenOtro;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        when(salesClient.obtenerPedido(10)).thenReturn(new PedidoResumen(10, "cliente01", "CONFIRMADO"));
        when(salesClient.obtenerPedido(11)).thenReturn(new PedidoResumen(11, "cliente01", "ENTREGADO"));
        tokenAdmin = "Bearer " + jwtService.generarToken("disenador", List.of("ROLE_ADMIN"));
        tokenCliente = "Bearer " + jwtService.generarToken("cliente01", List.of("ROLE_CLIENTE"));
        tokenOtro = "Bearer " + jwtService.generarToken("cliente02", List.of("ROLE_CLIENTE"));
    }

    private String body(int idpedido, String archivo) {
        return "{\"idpedido\":%d,\"nombreArchivo\":\"%s\",\"urlArchivo\":\"https://drive.example/%s\"}"
                .formatted(idpedido, archivo, archivo);
    }

    @Test
    @DisplayName("Nueva versión reemplaza la anterior; el cliente observa y luego aprueba")
    void versionesYAprobacion() throws Exception {
        mvc.perform(post("/api/v1/disenos").header("Authorization", tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content(body(10, "logo-v1.pdf")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.cliente").value("cliente01"));
        Integer v1 = repository.findAll().getFirst().getIddiseno();

        mvc.perform(post("/api/v1/disenos/" + v1 + "/decision").header("Authorization", tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"OBSERVAR\",\"comentario\":\"Cambiar color\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("OBSERVADO"));

        mvc.perform(post("/api/v1/disenos").header("Authorization", tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content(body(10, "logo-v2.pdf")))
                .andExpect(jsonPath("$.version").value(2));
        Integer v2 = repository.findByIdpedidoOrderByVersionDesc(10).getFirst().getIddiseno();

        // Otro cliente no puede ver ni aprobar
        mvc.perform(post("/api/v1/disenos/" + v2 + "/decision").header("Authorization", tokenOtro)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"APROBAR\"}"))
                .andExpect(status().isNotFound());
        // Aprobar una versión antigua no está permitido
        mvc.perform(post("/api/v1/disenos/" + v1 + "/decision").header("Authorization", tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"APROBAR\"}"))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/v1/disenos/" + v2 + "/decision").header("Authorization", tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"APROBAR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADO"));

        assertThat(repository.findByIdpedidoOrderByVersionDesc(10)).extracting(d -> d.getEstado().name())
                .containsExactly("APROBADO", "REEMPLAZADO");
    }

    @Test
    @DisplayName("Validaciones: formato de archivo, estado del pedido y rol")
    void validaciones() throws Exception {
        mvc.perform(post("/api/v1/disenos").header("Authorization", tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content(body(10, "virus.exe")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/disenos").header("Authorization", tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content(body(11, "logo.pdf")))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/disenos").header("Authorization", tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON).content(body(10, "logo.pdf")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT y DELETE solo sobre versiones en revisión")
    void editarYEliminar() throws Exception {
        mvc.perform(post("/api/v1/disenos").header("Authorization", tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON).content(body(10, "logo.pdf")));
        Integer id = repository.findAll().getFirst().getIddiseno();

        mvc.perform(put("/api/v1/disenos/" + id).header("Authorization", tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content(body(10, "logo-final.png")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreArchivo").value("logo-final.png"));
        mvc.perform(get("/api/v1/disenos?idpedido=10").header("Authorization", tokenCliente))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(delete("/api/v1/disenos/" + id).header("Authorization", tokenAdmin))
                .andExpect(status().isNoContent());
    }
}
