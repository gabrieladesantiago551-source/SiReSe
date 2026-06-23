package mx.ipn.upiiz.sirese.controllers;

import mx.ipn.upiiz.sirese.entities.Carrera;
import mx.ipn.upiiz.sirese.repositories.CarreraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/carreras")
@CrossOrigin(origins = "*")
public class CarreraController {

    @Autowired
    private CarreraRepository carreraRepository;

    // GET /api/carreras — Listar todas
    @GetMapping
    public List<Carrera> listar() {
        return carreraRepository.findAll();
    }

    // GET /api/carreras/{id} — Obtener una
    @GetMapping("/{id}")
    public ResponseEntity<Carrera> obtener(@PathVariable Long id) {
        return carreraRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/carreras — Crear nueva
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Carrera carrera) {
        Carrera nueva = carreraRepository.save(carrera);
        return ResponseEntity.ok(Map.of("success", true, "id", nueva.getId(), "mensaje", "Carrera creada correctamente."));
    }

    // PUT /api/carreras/{id} — Actualizar
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Carrera datos) {
        Optional<Carrera> opt = carreraRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Carrera carrera = opt.get();
        carrera.setNombre(datos.getNombre());
        carrera.setSemestres(datos.getSemestres());
        carrera.setObservaciones(datos.getObservaciones());
        carreraRepository.save(carrera);
        return ResponseEntity.ok(Map.of("success", true, "mensaje", "Carrera actualizada correctamente."));
    }

    // DELETE /api/carreras/{id} — Eliminar
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        if (!carreraRepository.existsById(id)) return ResponseEntity.notFound().build();
        carreraRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true, "mensaje", "Carrera eliminada correctamente."));
    }
}
