package com.exemplo.authservice.controller;

import com.exemplo.authservice.TestcontainersConfiguration;
import com.exemplo.authservice.repository.RefreshTokenRepository;
import com.exemplo.authservice.repository.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void prepararUsuario() throws Exception {
        refreshTokenRepository.deleteAll();
        usuarioRepository.deleteAll();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Teste","email":"teste@vendas.com","senha":"senha123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("teste@vendas.com"));
    }

    @Test
    void deveAutenticarERetornarAccessERefreshToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"teste@vendas.com","senha":"senha123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(120))
                .andExpect(jsonPath("$.refreshExpiresIn").value(1800));
    }

    @Test
    void deveRejeitarSenhaInvalida() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"teste@vendas.com","senha":"errada"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRejeitarUsuarioInexistente() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ninguem@vendas.com","senha":"senha123"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRejeitarCadastroComEmailDuplicado() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Outro","email":"teste@vendas.com","senha":"senha123"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void deveGerarNovosTokensComRefreshERotacionarORefreshAntigo() throws Exception {
        String tokensLogin = login();
        String refreshAntigo = JsonPath.read(tokensLogin, "$.refreshToken");
        String accessAntigo = JsonPath.read(tokensLogin, "$.accessToken");

        MvcResult resultado = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshAntigo + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        String novoRefresh = JsonPath.read(resultado.getResponse().getContentAsString(), "$.refreshToken");
        assertThat(novoRefresh).isNotEqualTo(refreshAntigo);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshAntigo + "\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + accessAntigo + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRejeitarRefreshTokenInvalido() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"token.invalido.qualquer"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRevogarRefreshNoLogout() throws Exception {
        String refresh = JsonPath.read(login(), "$.refreshToken");

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    private String login() throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"teste@vendas.com","senha":"senha123"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }
}
