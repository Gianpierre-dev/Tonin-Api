package org.example.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FraseControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void cuandoPidoFraseAleatoriaSinToken_DebePermitirlo() throws Exception {
        // El endpoint es público (no da 401/403), pero retorna 404 si no hay datos
        mockMvc.perform(get("/api/frases/random?animo=FELIZ"))
                .andExpect(status().isNotFound());
    }

    @Test
    void cuandoIntentoCrearFraseSinToken_DebeDar403Forbidden() throws Exception {
        mockMvc.perform(post("/api/frases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\": \"Frase de prueba\", \"estadoAnimoId\": 1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cuandoAccedoASwaggerSinToken_DebePermitirlo() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isFound());
    }
}
