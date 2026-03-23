package org.example.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UploadControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void subirImagenSinToken_DebeDar403() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "foto.png", "image/png", "contenido".getBytes()
        );

        mockMvc.perform(multipart("/api/uploads/imagen").file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    void subirMusicaSinToken_DebeDar403() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cancion.mp3", "audio/mpeg", "contenido".getBytes()
        );

        mockMvc.perform(multipart("/api/uploads/musica").file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    void eliminarArchivoSinToken_DebeDar403() throws Exception {
        mockMvc.perform(delete("/api/uploads").param("url", "https://example.com/file.png"))
                .andExpect(status().isForbidden());
    }
}
