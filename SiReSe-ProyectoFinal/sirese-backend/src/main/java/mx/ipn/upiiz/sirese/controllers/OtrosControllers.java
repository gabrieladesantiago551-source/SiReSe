package mx.ipn.upiiz.sirese.controllers;

import mx.ipn.upiiz.sirese.entities.Administrador;
import mx.ipn.upiiz.sirese.entities.Aspirante;
import mx.ipn.upiiz.sirese.repositories.AdministradorRepository;
import mx.ipn.upiiz.sirese.repositories.AspiranteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

// ============================================================
//  Auth Controller
// ============================================================
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
class AuthController {

    @Autowired
    private AdministradorRepository adminRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String usuario   = body.get("usuario");
        String contrasena = body.get("contrasena");

        Optional<Administrador> opt = adminRepository.findByUsuario(usuario);
        if (opt.isEmpty()) {
            return ResponseEntity.ok(Map.of("success", false, "mensaje", "Usuario no encontrado."));
        }

        Administrador admin = opt.get();
        // En producción usar BCrypt: passwordEncoder.matches(contrasena, admin.getContrasena())
        if (!admin.getContrasena().equals(contrasena)) {
            return ResponseEntity.ok(Map.of("success", false, "mensaje", "Contraseña incorrecta."));
        }

        return ResponseEntity.ok(Map.of(
            "success", true,
            "nombre", admin.getNombreCompleto(),
            "mensaje", "Sesión iniciada correctamente."
        ));
    }
}

// ============================================================
//  Mail Controller
// ============================================================
@RestController
@RequestMapping("/api/correo")
@CrossOrigin(origins = "*")
class CorreoController {

    @Autowired private JavaMailSender mailSender;
    @Autowired private AspiranteRepository aspiranteRepository;

    // POST /api/correo/individual
    @PostMapping("/individual")
    public ResponseEntity<?> enviarIndividual(@RequestBody Map<String, Object> body) {
        Long aspiranteId = Long.valueOf(body.get("aspiranteId").toString());
        String asunto  = (String) body.get("asunto");
        String mensaje = (String) body.get("mensaje");

        Optional<Aspirante> opt = aspiranteRepository.findById(aspiranteId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(opt.get().getEmail());
            msg.setSubject(asunto);
            msg.setText(mensaje);
            mailSender.send(msg);
            return ResponseEntity.ok(Map.of("success", true, "mensaje", "Correo enviado."));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "mensaje", "Error al enviar: " + e.getMessage()));
        }
    }

    // POST /api/correo/masivo
    @PostMapping("/masivo")
    public ResponseEntity<?> enviarMasivo(@RequestBody Map<String, String> body) {
        String asunto  = body.get("asunto");
        String mensaje = body.get("mensaje");

        List<Aspirante> aspirantes = aspiranteRepository.findAll();
        int enviados = 0;

        for (Aspirante a : aspirantes) {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setTo(a.getEmail());
                msg.setSubject(asunto);
                msg.setText("Estimado(a) " + a.getNombre() + ",\n\n" + mensaje +
                    "\n\nAtentamente,\nAdministración SiReSe - UPIIZ IPN");
                mailSender.send(msg);
                enviados++;
            } catch (Exception e) {
                System.err.println("Error enviando a " + a.getEmail() + ": " + e.getMessage());
            }
        }

        return ResponseEntity.ok(Map.of("success", true, "total", enviados,
            "mensaje", "Correo enviado a " + enviados + " aspirantes."));
    }
}

// ============================================================
//  Dashboard Controller
// ============================================================
@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
class DashboardController {

    @Autowired private AspiranteRepository aspiranteRepository;

    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        long totalAspirantes = aspiranteRepository.count();
        return ResponseEntity.ok(Map.of(
            "aspirantes",  totalAspirantes,
            "carreras",    4,
            "constancias", totalAspirantes,
            "correos",     totalAspirantes * 2
        ));
    }
}
