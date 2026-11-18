package org.example.service;

import org.example.model.Rol;
import org.example.model.Usuario;
import org.example.repository.RolRepository;
import org.example.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Inicialización al arrancar:
 * <ol>
 *   <li>crea los roles ROLE_ADMIN y ROLE_USER si faltan;</li>
 *   <li>si no hay ningún admin y están definidas ADMIN_USERNAME y ADMIN_PASSWORD, crea el admin.</li>
 * </ol>
 * Si ya existe un admin no toca nada (tampoco su contraseña). Así el primer admin no depende de
 * quién llegue primero a /api/auth/register.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    static final String ROLE_ADMIN = "ROLE_ADMIN";
    static final String ROLE_USER = "ROLE_USER";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public AdminBootstrap(UsuarioRepository usuarioRepository,
                          RolRepository rolRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${admin.username:}") String adminUsername,
                          @Value("${admin.password:}") String adminPassword) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername == null ? "" : adminUsername.trim();
        this.adminPassword = adminPassword == null ? "" : adminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Rol adminRol = asegurarRol(ROLE_ADMIN);
        asegurarRol(ROLE_USER);

        if (usuarioRepository.existsByRoles_Nombre(ROLE_ADMIN)) {
            return;
        }
        if (adminUsername.isEmpty() || adminPassword.isEmpty()) {
            log.warn("No existe ningún administrador. Define ADMIN_USERNAME y ADMIN_PASSWORD para crearlo al arrancar.");
            return;
        }
        if (adminUsername.length() < 3 || adminUsername.length() > 50) {
            throw new IllegalStateException("ADMIN_USERNAME debe tener entre 3 y 50 caracteres");
        }
        if (adminPassword.length() < Passwords.MIN_CARACTERES || Passwords.excedeLimiteBcrypt(adminPassword)) {
            throw new IllegalStateException("ADMIN_PASSWORD debe tener al menos " + Passwords.MIN_CARACTERES
                    + " caracteres y como máximo " + Passwords.MAX_BYTES + " bytes en UTF-8");
        }
        if (usuarioRepository.findByUsername(adminUsername).isPresent()) {
            // No se promueve en silencio una cuenta existente: podría ser de cualquiera.
            throw new IllegalStateException("ADMIN_USERNAME '" + adminUsername
                    + "' ya pertenece a un usuario que no es administrador. Usa otro nombre.");
        }

        Usuario admin = new Usuario(adminUsername, passwordEncoder.encode(adminPassword));
        admin.setRoles(Set.of(adminRol));
        usuarioRepository.save(admin);
        log.info("Administrador inicial creado: {}", adminUsername);
    }

    private Rol asegurarRol(String nombre) {
        return rolRepository.findByNombre(nombre).orElseGet(() -> rolRepository.save(new Rol(nombre)));
    }
}
