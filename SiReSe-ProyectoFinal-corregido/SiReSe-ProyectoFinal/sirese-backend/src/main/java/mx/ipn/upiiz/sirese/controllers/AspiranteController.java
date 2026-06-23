package mx.ipn.upiiz.sirese.controllers;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import mx.ipn.upiiz.sirese.entities.Aspirante;
import mx.ipn.upiiz.sirese.entities.Carrera;
import mx.ipn.upiiz.sirese.repositories.AspiranteRepository;
import mx.ipn.upiiz.sirese.repositories.CarreraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/aspirantes")
@CrossOrigin(origins = "*")
public class AspiranteController {

    @Autowired private AspiranteRepository aspiranteRepository;
    @Autowired private CarreraRepository carreraRepository;
    @Autowired private JavaMailSender mailSender;

    @Value("${sirese.admin.email}")
    private String adminEmail;

    // GET /api/aspirantes — Listar todos
    @GetMapping
    public List<Aspirante> listar() {
        return aspiranteRepository.findAll();
    }

    // GET /api/aspirantes/recientes — Para dashboard
    @GetMapping("/recientes")
    public List<Aspirante> recientes() {
        return aspiranteRepository.findTop5ByOrderByFechaRegistroDesc();
    }

    // GET /api/aspirantes/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Aspirante> obtener(@PathVariable Long id) {
        return aspiranteRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/aspirantes/check-email?email=... — Validar correo único con AJAX
    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestParam String email) {
        boolean existe = aspiranteRepository.existsByEmail(email);
        return ResponseEntity.ok(Map.of("exists", existe));
    }

    // POST /api/aspirantes/registrar — Registro público
    @PostMapping("/registrar")
    public ResponseEntity<?> registrar(@RequestBody Map<String, Object> body) {
        String email = (String) body.get("email");

        if (aspiranteRepository.existsByEmail(email)) {
            return ResponseEntity.ok(Map.of("success", false, "mensaje", "El correo ya está registrado."));
        }

        Long carreraId = Long.valueOf(body.get("carreraId").toString());
        Optional<Carrera> carreraOpt = carreraRepository.findById(carreraId);
        if (carreraOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("success", false, "mensaje", "Carrera no encontrada."));
        }

        Aspirante a = new Aspirante();
        a.setNombre((String) body.get("nombre"));
        a.setTelefono((String) body.get("telefono"));
        a.setEmail(email);
        a.setCarrera(carreraOpt.get());
        aspiranteRepository.save(a);

        // Notificar al administrador por correo
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(adminEmail);
            msg.setSubject("SiReSe: Nuevo aspirante registrado");
            msg.setText("Se registró un nuevo aspirante:\n\nNombre: " + a.getNombre()
                + "\nCorreo: " + a.getEmail()
                + "\nTeléfono: " + a.getTelefono()
                + "\nCarrera: " + a.getCarrera().getNombre()
                + "\nFecha: " + a.getFechaRegistro());
            mailSender.send(msg);
        } catch (Exception e) {
            // No interrumpir el registro si falla el correo
            System.err.println("Error al enviar correo: " + e.getMessage());
        }

        return ResponseEntity.ok(Map.of("success", true, "mensaje", "Registro exitoso."));
    }

    // PUT /api/aspirantes/{id} — Actualizar datos del aspirante
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Optional<Aspirante> opt = aspiranteRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Aspirante a = opt.get();

        String nuevoEmail = (String) body.get("email");
        if (nuevoEmail != null && !nuevoEmail.equalsIgnoreCase(a.getEmail())
                && aspiranteRepository.existsByEmail(nuevoEmail)) {
            return ResponseEntity.ok(Map.of("success", false, "mensaje", "El correo ya está registrado por otro aspirante."));
        }

        if (body.get("carreraId") != null) {
            Long carreraId = Long.valueOf(body.get("carreraId").toString());
            Optional<Carrera> carreraOpt = carreraRepository.findById(carreraId);
            if (carreraOpt.isEmpty()) {
                return ResponseEntity.ok(Map.of("success", false, "mensaje", "Carrera no encontrada."));
            }
            a.setCarrera(carreraOpt.get());
        }

        if (body.get("nombre") != null) a.setNombre((String) body.get("nombre"));
        if (body.get("telefono") != null) a.setTelefono((String) body.get("telefono"));
        if (nuevoEmail != null) a.setEmail(nuevoEmail);

        aspiranteRepository.save(a);
        return ResponseEntity.ok(Map.of("success", true, "mensaje", "Aspirante actualizado correctamente."));
    }

    // DELETE /api/aspirantes/{id} — Eliminar aspirante
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        if (!aspiranteRepository.existsById(id)) return ResponseEntity.notFound().build();
        aspiranteRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true, "mensaje", "Aspirante eliminado correctamente."));
    }

    // GET /api/aspirantes/{id}/constancia.pdf — Generar constancia con OpenPDF
    @GetMapping("/{id}/constancia.pdf")
    public ResponseEntity<byte[]> generarConstancia(@PathVariable Long id) {
        Optional<Aspirante> opt = aspiranteRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Aspirante a = opt.get();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.LETTER);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // Encabezado
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font boldFont  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

            Paragraph header = new Paragraph();
            header.setAlignment(Element.ALIGN_CENTER);
            header.add(new Chunk("Instituto Politécnico Nacional\n", boldFont));
            header.add(new Chunk("Unidad Profesional Interdisciplinaria de Ingeniería campus Zacatecas\n", normalFont));
            header.add(new Chunk("Unidad de Educación Continua\n\n", normalFont));
            doc.add(header);

            doc.add(new Paragraph("CONSTANCIA", titleFont));
            doc.add(new Paragraph("A QUIEN CORRESPONDA:\n\n", boldFont));

            doc.add(new Paragraph(
                "Por medio de la presente se hace constar que:\n\n", normalFont));

            Paragraph nombre = new Paragraph(a.getNombre(), boldFont);
            nombre.setAlignment(Element.ALIGN_CENTER);
            doc.add(nombre);

            doc.add(new Paragraph(
                "\ncon correo electrónico " + a.getEmail() +
                ", se encuentra registrado(a) como aspirante en la carrera de:\n", normalFont));

            Paragraph carrera = new Paragraph(a.getCarrera().getNombre(), boldFont);
            carrera.setAlignment(Element.ALIGN_CENTER);
            doc.add(carrera);

            doc.add(new Paragraph(
                "\nFecha de registro: " + a.getFechaRegistro() +
                "\n\nLo anterior se hace constar para los fines que al interesado convengan.\n\n\n\n",
                normalFont));

            Paragraph firma = new Paragraph("_______________________________\nAdministrador del Sistema\nSiReSe - UPIIZ IPN", normalFont);
            firma.setAlignment(Element.ALIGN_CENTER);
            doc.add(firma);

            doc.close();

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=constancia_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(baos.toByteArray());

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
