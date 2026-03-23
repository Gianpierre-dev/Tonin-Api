package org.example.service;

import org.example.dto.AuthResponse;
import org.example.dto.LoginRequest;
import org.example.dto.RegisterRequest;
import org.example.exception.BadRequestException;
import org.example.model.Rol;
import org.example.model.Usuario;
import org.example.repository.RolRepository;
import org.example.repository.UsuarioRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final IJwtService jwtService;
    private final UserDetailsService userDetailsService;

    public AuthService(AuthenticationManager authenticationManager,
                       UsuarioRepository usuarioRepository,
                       RolRepository rolRepository,
                       PasswordEncoder passwordEncoder,
                       IJwtService jwtService,
                       UserDetailsService userDetailsService) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Transactional
    public String register(RegisterRequest request) {
        if (usuarioRepository.findByUsername(request.username()).isPresent()) {
            throw new BadRequestException("El usuario ya existe");
        }

        inicializarRolesSiNoExisten();

        Usuario usuario = new Usuario(
                request.username(),
                passwordEncoder.encode(request.password())
        );

        // El primer usuario del sistema se convierte en ADMIN
        if (usuarioRepository.count() == 0) {
            Rol adminRol = rolRepository.findByNombre("ROLE_ADMIN")
                    .orElseThrow(() -> new BadRequestException("Rol ADMIN no encontrado"));
            usuario.setRoles(Set.of(adminRol));
        } else {
            Rol userRol = rolRepository.findByNombre("ROLE_USER")
                    .orElseThrow(() -> new BadRequestException("Rol USER no encontrado"));
            usuario.setRoles(Set.of(userRol));
        }

        usuarioRepository.save(usuario);

        String rolAsignado = usuario.getRoles().iterator().next().getNombre();
        return "Usuario registrado con éxito. Rol asignado: " + rolAsignado;
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());
        String jwt = jwtService.generateToken(userDetails);
        return new AuthResponse(jwt);
    }

    private void inicializarRolesSiNoExisten() {
        if (rolRepository.count() == 0) {
            rolRepository.save(new Rol("ROLE_ADMIN"));
            rolRepository.save(new Rol("ROLE_USER"));
        }
    }
}
