package pe.sigip.design.controller;
import pe.sigip.design.model.Diseno; import pe.sigip.design.repository.DisenoRepository; import jakarta.validation.Valid; import org.springframework.http.HttpStatus; import org.springframework.web.bind.annotation.*; import org.springframework.web.server.ResponseStatusException; import java.util.List; import java.util.Map;
@RestController @RequestMapping("/api/disenos") @CrossOrigin(origins="http://localhost:4200")
public class DisenoController {
 private final DisenoRepository repo; public DisenoController(DisenoRepository repo){this.repo=repo;}
 @GetMapping public List<Diseno> listar(){return repo.findAll();}
 @GetMapping("/{id}") public Diseno buscar(@PathVariable Long id){return repo.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Diseño no encontrado"));}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public Diseno crear(@Valid @RequestBody Diseno d){d.setId(null);return repo.save(d);}
 @PutMapping("/{id}") public Diseno actualizar(@PathVariable Long id,@Valid @RequestBody Diseno datos){Diseno d=buscar(id);d.setNombre(datos.getNombre());d.setUrlArchivo(datos.getUrlArchivo());d.setEstado(datos.getEstado());d.setVersion(datos.getVersion());return repo.save(d);}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void eliminar(@PathVariable Long id){if(!repo.existsById(id))throw new ResponseStatusException(HttpStatus.NOT_FOUND);repo.deleteById(id);}
 @GetMapping("/health") public Map<String,String> health(){return Map.of("servicio","SIGIP Design Service","estado","activo");}
}