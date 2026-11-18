package org.example.service;

import org.example.model.Rol;
import org.example.model.Usuario;
import org.example.repository.RolRepository;
import org.example.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Hallazgo 11: el primer admin sale del entorno (ADMIN_USERNAME / ADMIN_PASSWORD) al arrancar,
 * no del primer registro anónimo.
 */
class AdminBootstrapTest {

    private UsuarioRepository usuarioRepository;
    private RolRepository rolRepository;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        rolRepository = mock(RolRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(rolRepository.findByNombre("ROLE_ADMIN")).thenReturn(Optional.of(new Rol("ROLE_ADMIN")));
        when(rolRepository.findByNombre("ROLE_USER")).thenReturn(Optional.of(new Rol("ROLE_USER")));
    }

    private AdminBootstrap bootstrap(String username, String password) {
        return new AdminBootstrap(usuarioRepository, rolRepository, passwordEncoder, username, password);
    }

    @Test
    void creaLosRolesQueFaltan() {
        when(rolRepository.findByNombre("ROLE_USER")).thenReturn(Optional.empty());
        when(usuarioRepository.existsByRoles_Nombre("ROLE_ADMIN")).thenReturn(true);

        bootstrap("", "").run(null);

        ArgumentCaptor<Rol> creado = ArgumentCaptor.forClass(Rol.class);
        verify(rolRepository).save(creado.capture());
        assertEquals("ROLE_USER", creado.getValue().getNombre());
    }

    @Test
    void sinAdmin_ConCredenciales_CreaElAdmin() {
        when(usuarioRepository.existsByRoles_Nombre("ROLE_ADMIN")).thenReturn(false);
        when(usuarioRepository.findByUsername("jefe")).thenReturn(Optional.empty());

        bootstrap("jefe", "una-clave-segura").run(null);

        ArgumentCaptor<Usuario> creado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(creado.capture());
        assertEquals("jefe", creado.getValue().getUsername());
        assertEquals("encoded", creado.getValue().getPassword());
        assertEquals(List.of("ROLE_ADMIN"), creado.getValue().getRoles().stream().map(Rol::getNombre).toList());
    }

    @Test
    void conAdminExistente_NoCreaNiModificaNada() {
        when(usuarioRepository.existsByRoles_Nombre("ROLE_ADMIN")).thenReturn(true);

        bootstrap("jefe", "una-clave-segura").run(null);

        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void sinAdmin_SinCredenciales_NoCreaNada() {
        when(usuarioRepository.existsByRoles_Nombre("ROLE_ADMIN")).thenReturn(false);

        bootstrap("", "").run(null);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void sinAdmin_UsernameYaUsadoPorOtroUsuario_FallaElArranque() {
        // No se promueve en silencio una cuenta existente a admin.
        when(usuarioRepository.existsByRoles_Nombre("ROLE_ADMIN")).thenReturn(false);
        when(usuarioRepository.findByUsername("jefe")).thenReturn(Optional.of(new Usuario("jefe", "x")));

        assertThrows(IllegalStateException.class, () -> bootstrap("jefe", "una-clave-segura").run(null));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void sinAdmin_PasswordInvalida_FallaElArranque() {
        when(usuarioRepository.existsByRoles_Nombre("ROLE_ADMIN")).thenReturn(false);
        when(usuarioRepository.findByUsername("jefe")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> bootstrap("jefe", "corta").run(null));
        assertThrows(IllegalStateException.class, () -> bootstrap("jefe", "ñ".repeat(37)).run(null));
        verify(usuarioRepository, never()).save(any());
    }
}
