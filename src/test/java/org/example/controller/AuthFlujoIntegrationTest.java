package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.Rol;
import org.example.model.Usuario;
import org.example.repository.RolRepository;
import org.example.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo completo de autenticación (hallazgos 11 y 13 de la auditoría): admin inicial desde el
 * entorno, registro sin privilegios y token atado al id del usuario.
 * BD H2 propia para no compartir usuarios con los demás contextos de test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:authflujo;DB_CLOSE_DELAY=-1",
        "admin.username=jefe",
        "admin.password=clave-segura-123",
        "auth.registro.habilitado=true"
})
class AuthFlujoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    private Usuario crearAdmin(String username, String password) {
        Rol admin = rolRepository.findByNombre("ROLE_ADMIN").orElseThrow();
        Usuario usuario = new Usuario(username, passwordEncoder.encode(password));
        usuario.setRoles(Set.of(admin));
        return usuarioRepository.save(usuario);
    }

    @Test
    void elAdminInicialSeCreaDesdeElEntorno_YAccedeAEndpointsProtegidos() throws Exception {
        String token = login("jefe", "clave-segura-123");

        mockMvc.perform(get("/api/frases").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void elRegistroCreaUsuarioSinPrivilegios() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"ana\", \"password\": \"password123\"}"))
                .andExpect(status().isOk());
        assertThat(usuarioRepository.findByUsername("ana").orElseThrow().getRoles())
                .extracting(Rol::getNombre).containsExactly("ROLE_USER");

        String token = login("ana", "password123");
        mockMvc.perform(get("/api/frases").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void tokenDeUnUsuarioBorrado_NoSirveParaOtroConElMismoUsername() throws Exception {
        Usuario original = crearAdmin("temporal", "password123");
        String tokenViejo = login("temporal", "password123");
        mockMvc.perform(get("/api/frases").header("Authorization", "Bearer " + tokenViejo))
                .andExpect(status().isOk());

        usuarioRepository.delete(original);
        crearAdmin("temporal", "otra-clave-456");

        mockMvc.perform(get("/api/frases").header("Authorization", "Bearer " + tokenViejo))
                .andExpect(status().isUnauthorized());
    }
}
