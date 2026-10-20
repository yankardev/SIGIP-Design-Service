package pe.edu.cibertec.sigip.designservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.edu.cibertec.sigip.common.security.UsuarioActual;
import pe.edu.cibertec.sigip.designservice.dto.DecisionRequest;
import pe.edu.cibertec.sigip.designservice.dto.DisenoRequest;
import pe.edu.cibertec.sigip.designservice.dto.DisenoResponse;
import pe.edu.cibertec.sigip.designservice.service.DisenoService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/disenos")
@RequiredArgsConstructor
public class DisenoController {

    private final DisenoService service;

    @GetMapping
    public ResponseEntity<List<DisenoResponse>> listar(@RequestParam(required = false) Integer idpedido,
                                                       Authentication auth) {
        return ResponseEntity.ok(service.listar(idpedido, UsuarioActual.de(auth)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DisenoResponse> obtener(@PathVariable Integer id, Authentication auth) {
        return ResponseEntity.ok(service.obtener(id, UsuarioActual.de(auth)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DisenoResponse> registrar(@Valid @RequestBody DisenoRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(request, UsuarioActual.de(auth)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DisenoResponse> actualizar(@PathVariable Integer id, @Valid @RequestBody DisenoRequest request) {
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /** El cliente aprueba u observa la versión vigente de su diseño. */
    @PostMapping("/{id}/decision")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<DisenoResponse> decidir(@PathVariable Integer id,
                                                  @Valid @RequestBody DecisionRequest request,
                                                  Authentication auth) {
        return ResponseEntity.ok(service.decidir(id, request, UsuarioActual.de(auth)));
    }
}
