package org.example.service;

import org.example.dto.AuthResponse;
import org.example.dto.LoginRequest;
import org.example.dto.RegisterRequest;
import org.example.exception.BadRequestException;
import org.example.exception.ForbiddenException;
import org.example.model.Rol;
import org.example.model.Usuario;
import org.example.repository.RolRepository;
import org.example.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final IJwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final MessageSource messageSource;
    private final boolean registroHabilitado;
    /** Username reservado para el admin inicial (ADMIN_USERNAME): el registro no puede usarlo. */
    private final String adminUsername;

    public AuthService(AuthenticationManager authenticationManager,
                       UsuarioRepository usuarioRepository,
                       RolRepository rolRepository,
                       PasswordEncoder passwordEncoder,
                       IJwtService jwtService,
                       UserDetailsService userDetailsService,
                       MessageSource messageSource,
                       @Value("${auth.registro.habilitado:false}") boolean registroHabilitado,
                       @Value("${admin.username:}") String adminUsername) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.messageSource = messageSource;
        this.registroHabilitado = registroHabilitado;
        this.adminUsername = adminUsername == null ? "" : adminUsername.trim();
    }

    /**
     * Registro público. Está deshabilitado por defecto (auth.registro.habilitado) y nunca crea
     * administradores: el admin inicial lo crea {@link AdminBootstrap} desde el entorno.
     *
     * <p>Sin transacción SERIALIZABLE: el hash de BCrypt (cientos de ms) se calcula fuera de
     * cualquier transacción y, si dos registros simultáneos usan el mismo username, la restricción
     * única de la BD decide y el perdedor recibe error.user.exists en vez de un 500.</p>
     */
    public String register(RegisterRequest request) {
        if (!registroHabilitado) {
            throw new ForbiddenException("error.register.disabled");
        }
        if (Passwords.excedeLimiteBcrypt(request.password())) {
            throw new BadRequestException("error.password.bytes", Passwords.MAX_BYTES);
        }
        // El username del admin queda reservado: si alguien lo registrara antes de que exista el
        // admin, AdminBootstrap no podría crearlo y la app no arrancaría.
        if (!adminUsername.isEmpty() && adminUsername.equalsIgnoreCase(request.username().trim())) {
            log.warn("Intento de registro con el username reservado del administrador");
            throw new BadRequestException("error.user.exists");
        }
        if (usuarioRepository.findByUsername(request.username()).isPresent()) {
            log.warn("Intento de registro con username existente: {}", request.username());
            throw new BadRequestException("error.user.exists");
        }

        Rol userRol = rolRepository.findByNombre(AdminBootstrap.ROLE_USER)
                .orElseThrow(() -> new BadRequestException("error.role.user.notfound"));
        Usuario usuario = new Usuario(request.username(), passwordEncoder.encode(request.password()));
        usuario.setRoles(Set.of(userRol));

        try {
            usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException e) {
            // Lo esperable: otro registro con el mismo username ganó la carrera entre el chequeo y
            // el insert. Se loguea la causa por si fuera otra restricción.
            log.warn("Violación de integridad al registrar '{}': {}", request.username(),
                    e.getMostSpecificCause().getMessage());
            throw new BadRequestException("error.user.exists");
        }

        log.info("Usuario registrado: {} con rol {}", usuario.getUsername(), userRol.getNombre());
        return messageSource.getMessage("success.register", new Object[]{userRol.getNombre()},
                LocaleContextHolder.getLocale());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());
        String jwt = jwtService.generateToken(userDetails);
        return new AuthResponse(jwt);
    }
}
