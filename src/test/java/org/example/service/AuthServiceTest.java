package org.example.service;

import org.example.dto.AuthResponse;
import org.example.dto.LoginRequest;
import org.example.dto.RegisterRequest;
import org.example.exception.BadRequestException;
import org.example.model.Rol;
import org.example.model.Usuario;
import org.example.repository.RolRepository;
import org.example.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private AuthenticationManager authenticationManager;
    private UsuarioRepository usuarioRepository;
    private RolRepository rolRepository;
    private PasswordEncoder passwordEncoder;
    private IJwtService jwtService;
    private UserDetailsService userDetailsService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authenticationManager = Mockito.mock(AuthenticationManager.class);
        usuarioRepository = Mockito.mock(UsuarioRepository.class);
        rolRepository = Mockito.mock(RolRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        jwtService = Mockito.mock(IJwtService.class);
        userDetailsService = Mockito.mock(UserDetailsService.class);

        authService = new AuthService(
                authenticationManager, usuarioRepository, rolRepository,
                passwordEncoder, jwtService, userDetailsService
        );
    }

    @Test
    void registrarPrimerUsuario_DebeSerAdmin() {
        RegisterRequest request = new RegisterRequest("admin", "password123");
        Rol adminRol = new Rol("ROLE_ADMIN");

        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(rolRepository.count()).thenReturn(0L);
        when(usuarioRepository.count()).thenReturn(0L);
        when(rolRepository.findByNombre("ROLE_ADMIN")).thenReturn(Optional.of(adminRol));
        when(passwordEncoder.encode("password123")).thenReturn("encoded");

        String resultado = authService.register(request);

        assertTrue(resultado.contains("ROLE_ADMIN"));
        verify(rolRepository, times(2)).save(any(Rol.class));
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void registrarSegundoUsuario_DebeSerUser() {
        RegisterRequest request = new RegisterRequest("user1", "password123");
        Rol userRol = new Rol("ROLE_USER");

        when(usuarioRepository.findByUsername("user1")).thenReturn(Optional.empty());
        when(rolRepository.count()).thenReturn(2L);
        when(usuarioRepository.count()).thenReturn(1L);
        when(rolRepository.findByNombre("ROLE_USER")).thenReturn(Optional.of(userRol));
        when(passwordEncoder.encode("password123")).thenReturn("encoded");

        String resultado = authService.register(request);

        assertTrue(resultado.contains("ROLE_USER"));
    }

    @Test
    void registrarUsuarioExistente_DebeLanzarExcepcion() {
        RegisterRequest request = new RegisterRequest("admin", "password123");

        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(new Usuario()));

        assertThrows(BadRequestException.class, () -> authService.register(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void loginCorrecto_DebeRetornarToken() {
        LoginRequest request = new LoginRequest("admin", "password123");
        UserDetails userDetails = new User("admin", "encoded",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        when(userDetailsService.loadUserByUsername("admin")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token-123");

        AuthResponse response = authService.login(request);

        assertEquals("jwt-token-123", response.token());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void loginConCredencialesInvalidas_DebeLanzarExcepcion() {
        LoginRequest request = new LoginRequest("admin", "wrong");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }
}
