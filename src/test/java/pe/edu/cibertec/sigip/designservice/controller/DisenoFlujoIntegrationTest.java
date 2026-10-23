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
import pe.edu.cibertec.sigip.designservice.repository.DisenoRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Versiones y validación interna del diseño antes de producción. */
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

    private String tokenOperaciones;
    private String tokenVentas;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        when(salesClient.obtenerPedido(10))
                .thenReturn(new PedidoResumen(10, "Comercial Norte SAC", "CONFIRMADO"),
                            new PedidoResumen(10, "Comercial Norte SAC", "EN_DISENO"));
        when(salesClient.obtenerPedido(11))
                .thenReturn(new PedidoResumen(11, "Comercial Norte SAC", "ENTREGADO"));
        tokenOperaciones = "Bearer " + jwtService.generarToken("operaciones01", List.of("ROLE_OPERACIONES"));
        tokenVentas = "Bearer " + jwtService.generarToken("ventas01", List.of("ROLE_VENTAS"));
    }

    private String body(int idpedido, String archivo) {
        return "{\"idpedido\":%d,\"nombreArchivo\":\"%s\",\"urlArchivo\":\"https://drive.example/%s\"}"
                .formatted(idpedido, archivo, archivo);
    }

    @Test
    @DisplayName("Operaciones versiona, observa y aprueba internamente el arte")
    void versionesYValidacionInterna() throws Exception {
        mvc.perform(post("/api/v1/disenos").header("Authorization", tokenOperaciones)
                        .contentType(MediaType.APPLICATION_JSON).content(body(10, "logo-v1.pdf")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.cliente").value("Comercial Norte SAC"));
        Integer v1 = repository.findAll().getFirst().getIddiseno();

        mvc.perform(post("/api/v1/disenos/" + v1 + "/decision").header("Authorization", tokenOperaciones)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"OBSERVAR\",\"comentario\":\"Ajustar color corporativo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("OBSERVADO"));

        mvc.perform(post("/api/v1/disenos").header("Authorization", tokenOperaciones)
                        .contentType(MediaType.APPLICATION_JSON).content(body(10, "logo-v2.pdf")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(2));
        Integer v2 = repository.findByIdpedidoOrderByVersionDesc(10).getFirst().getIddiseno();

        mvc.perform(post("/api/v1/disenos/" + v1 + "/decision").header("Authorization", tokenOperaciones)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"APROBAR\"}"))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/v1/disenos/" + v2 + "/decision").header("Authorization", tokenOperaciones)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"APROBAR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADO"));

        assertThat(repository.findByIdpedidoOrderByVersionDesc(10)).extracting(d -> d.getEstado().name())
                .containsExactly("APROBADO", "REEMPLAZADO");
    }

    @Test
    @DisplayName("Validaciones: formato, estado del pedido y rol interno")
    void validaciones() throws Exception {
        mvc.perform(post("/api/v1/disenos").header("Authorization", tokenOperaciones)
                        .contentType(MediaType.APPLICATION_JSON).content(body(10, "virus.exe")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/disenos").header("Authorization", tokenOperaciones)
                        .contentType(MediaType.APPLICATION_JSON).content(body(11, "logo.pdf")))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/disenos").header("Authorization", tokenVentas)
                        .contentType(MediaType.APPLICATION_JSON).content(body(10, "logo.pdf")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT y DELETE solo sobre versiones en revisión")
    void editarYEliminar() throws Exception {
        mvc.perform(post("/api/v1/disenos").header("Authorization", tokenOperaciones)
                .contentType(MediaType.APPLICATION_JSON).content(body(10, "logo.pdf")));
        Integer id = repository.findAll().getFirst().getIddiseno();

        mvc.perform(put("/api/v1/disenos/" + id).header("Authorization", tokenOperaciones)
                        .contentType(MediaType.APPLICATION_JSON).content(body(10, "logo-final.png")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreArchivo").value("logo-final.png"));
        mvc.perform(get("/api/v1/disenos?idpedido=10").header("Authorization", tokenOperaciones))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(delete("/api/v1/disenos/" + id).header("Authorization", tokenOperaciones))
                .andExpect(status().isNoContent());
    }
}
