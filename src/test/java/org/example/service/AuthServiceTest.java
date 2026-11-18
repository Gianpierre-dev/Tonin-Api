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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private AuthenticationManager authenticationManager;
    private UsuarioRepository usuarioRepository;
    private RolRepository rolRepository;
    private PasswordEncoder passwordEncoder;
    private IJwtService jwtService;
    private UserDetailsService userDetailsService;
    private MessageSource messageSource;

    @BeforeEach
    void setUp() {
        authenticationManager = Mockito.mock(AuthenticationManager.class);
        usuarioRepository = Mockito.mock(UsuarioRepository.class);
        rolRepository = Mockito.mock(RolRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        jwtService = Mockito.mock(IJwtService.class);
        userDetailsService = Mockito.mock(UserDetailsService.class);
        messageSource = Mockito.mock(MessageSource.class);

        // El mensaje de éxito se resuelve vía MessageSource; embebemos el rol en la
        // respuesta para poder verificarlo en los asserts.
        when(messageSource.getMessage(eq("success.register"), any(), any(Locale.class)))
                .thenAnswer(inv -> "Rol asignado: " + ((Object[]) inv.getArgument(1))[0]);
        when(rolRepository.findByNombre("ROLE_USER")).thenReturn(Optional.of(new Rol("ROLE_USER")));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
    }

    private AuthService authService(boolean registroHabilitado) {
        return new AuthService(authenticationManager, usuarioRepository, rolRepository,
                passwordEncoder, jwtService, userDetailsService, messageSource, registroHabilitado);
    }

    // --- Hallazgo 11: el registro público no crea admins y está deshabilitado por defecto ---

    @Test
    void registroDeshabilitado_SeRechazaSinTocarLaBD() {
        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> authService(false).register(new RegisterRequest("ana", "password123")));

        assertEquals("error.register.disabled", ex.getMessage());
        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    @Test
    void registroHabilitado_ConLaBDVacia_CreaUsuarioComunNoAdmin() {
        when(usuarioRepository.findByUsername("primero")).thenReturn(Optional.empty());
        when(usuarioRepository.count()).thenReturn(0L);

        String resultado = authService(true).register(new RegisterRequest("primero", "password123"));

        assertTrue(resultado.contains("ROLE_USER"));
        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(guardado.capture());
        assertEquals("encoded", guardado.getValue().getPassword());
        assertEquals(List.of("ROLE_USER"), guardado.getValue().getRoles().stream().map(Rol::getNombre).toList());
        verify(rolRepository, never()).findByNombre("ROLE_ADMIN");
    }

    @Test
    void registrarUsuarioExistente_DebeLanzarExcepcion() {
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(new Usuario()));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> authService(true).register(new RegisterRequest("admin", "password123")));

        assertEquals("error.user.exists", ex.getMessage());
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    // --- Hallazgo 10: un registro concurrente con el mismo username no debe responder 500 ---

    @Test
    void registroConcurrenteMismoUsername_DevuelveUsuarioExistente() {
        when(usuarioRepository.findByUsername("ana")).thenReturn(Optional.empty());
        when(usuarioRepository.saveAndFlush(any(Usuario.class)))
                .thenThrow(new DataIntegrityViolationException("uk_usuarios_username"));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> authService(true).register(new RegisterRequest("ana", "password123")));

        assertEquals("error.user.exists", ex.getMessage());
    }

    // --- Hallazgo 12: BCrypt solo usa los primeros 72 bytes ---

    @Test
    void passwordDeMasDe72Bytes_SeRechaza() {
        // 37 caracteres "ñ" = 74 bytes en UTF-8, aunque son menos de 72 caracteres.
        RegisterRequest request = new RegisterRequest("ana", "ñ".repeat(37));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService(true).register(request));

        assertEquals("error.password.bytes", ex.getMessage());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void passwordDeExactamente72Bytes_SeAcepta() {
        when(usuarioRepository.findByUsername("ana")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> authService(true).register(new RegisterRequest("ana", "ñ".repeat(36))));
    }

    // --- Login (sin cambios de comportamiento) ---

    @Test
    void loginCorrecto_DebeRetornarToken() {
        LoginRequest request = new LoginRequest("admin", "password123");
        UserDetails userDetails = new User("admin", "encoded",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        when(userDetailsService.loadUserByUsername("admin")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token-123");

        AuthResponse response = authService(false).login(request);

        assertEquals("jwt-token-123", response.token());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void loginConCredencialesInvalidas_DebeLanzarExcepcion() {
        LoginRequest request = new LoginRequest("admin", "wrong");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService(false).login(request));
    }
}
