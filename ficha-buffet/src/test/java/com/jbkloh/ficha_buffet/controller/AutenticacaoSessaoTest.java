package com.jbkloh.ficha_buffet.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

import com.jbkloh.ficha_buffet.TestRsaKeys;
import com.jbkloh.ficha_buffet.exceptions.RefreshTokenInvalidoException;
import com.jbkloh.ficha_buffet.model.PermissoesUsuarioEntity;
import com.jbkloh.ficha_buffet.model.RefreshTokenEntity;
import com.jbkloh.ficha_buffet.model.RefreshTokenEntity.MotivoRevogacao;
import com.jbkloh.ficha_buffet.model.UsuarioEntity;
import com.jbkloh.ficha_buffet.repositories.PermissoesUsuarioRepository;
import com.jbkloh.ficha_buffet.repositories.RefreshTokenRepository;
import com.jbkloh.ficha_buffet.repositories.UsuarioRepository;
import com.jbkloh.ficha_buffet.service.RefreshTokenService;

import jakarta.servlet.http.Cookie;

/**
 * Sessão com access token (20 min) + refresh token (7 dias) rotacionado: login, renovação,
 * reuso, logout e o contrato de erros 401/403 com {@code code}, pelos endpoints reais.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AutenticacaoSessaoTest {

    @DynamicPropertySource
    static void chavesRsa(DynamicPropertyRegistry registry) {
        TestRsaKeys.registrar(registry);
    }

    private static final String SENHA = "senha-de-teste";

    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PermissoesUsuarioRepository permissoesRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtEncoder jwtEncoder;
    @Autowired private TransactionTemplate transacao;

    private String admin;
    private String comum;

    @BeforeEach
    void usuarios() {
        admin = criarUsuario("ROLE_ADMIN");
        comum = criarUsuario("ROLE_USER");
    }

    // ---- login -------------------------------------------------------------------------

    @Test
    void loginEmiteOsDoisCookiesEGravaSoOHashDoRefreshToken() throws Exception {
        MvcResult res = login(admin, null);

        String acesso = setCookie(res, "accessToken");
        String renovacao = setCookie(res, "refreshToken");
        assertThat(acesso).contains("HttpOnly", "SameSite=Strict", "Path=/", "Max-Age=1200");
        assertThat(renovacao).contains("HttpOnly", "SameSite=Strict", "Path=/", "Max-Age=604800");

        String bruto = valor(res, "refreshToken");
        assertThat(bruto).hasSize(43); // 32 bytes em base64url sem padding
        RefreshTokenEntity gravado = refreshTokenRepository.findByTokenHash(RefreshTokenService.hash(bruto)).orElseThrow();
        assertThat(gravado.getTokenHash()).hasSize(64).isNotEqualTo(bruto);
        assertThat(gravado.getRevogadoEm()).isNull();
        assertThat(gravado.getExpiraEm()).isBetween(Instant.now().plus(6, ChronoUnit.DAYS), Instant.now().plus(8, ChronoUnit.DAYS));
        assertThat(refreshTokenRepository.findAll()).noneMatch(t -> bruto.equals(t.getTokenHash()));
    }

    @Test
    void accessTokenDoLoginAutorizaRotaProtegida() throws Exception {
        MvcResult res = login(admin, null);
        mvc.perform(get("/equipe/retornarTodos").cookie(new Cookie("accessToken", valor(res, "accessToken"))))
            .andExpect(status().isOk());
    }

    @Test
    void senhaErradaDa401ComInvalidCredentials() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + admin + "\",\"password\":\"errada\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void novoLoginNoMesmoNavegadorEncerraASessaoAnteriorMasNaoAsDeOutrosDispositivos() throws Exception {
        String outroDispositivo = valor(login(admin, null), "refreshToken");
        String anterior = valor(login(admin, null), "refreshToken");

        String novo = valor(login(admin, anterior), "refreshToken");

        assertThat(registro(anterior).getMotivoRevogacao()).isEqualTo(MotivoRevogacao.NOVO_LOGIN);
        assertThat(registro(novo).getRevogadoEm()).isNull();
        assertThat(registro(outroDispositivo).getRevogadoEm()).isNull();
    }

    // ---- refresh -----------------------------------------------------------------------

    @Test
    void refreshRotacionaOTokenEEmiteNovoAccessToken() throws Exception {
        String original = valor(login(admin, null), "refreshToken");

        MvcResult res = refresh(original).andExpect(status().isOk()).andReturn();
        String novo = valor(res, "refreshToken");
        String acesso = valor(res, "accessToken");

        assertThat(novo).isNotEqualTo(original);
        RefreshTokenEntity antigo = registro(original);
        RefreshTokenEntity atual = registro(novo);
        assertThat(antigo.getMotivoRevogacao()).isEqualTo(MotivoRevogacao.ROTACIONADO);
        assertThat(atual.getRevogadoEm()).isNull();
        assertThat(atual.getSessaoId()).isEqualTo(antigo.getSessaoId());
        mvc.perform(get("/equipe/retornarTodos").cookie(new Cookie("accessToken", acesso))).andExpect(status().isOk());
    }

    @Test
    void refreshIgnoraAccessTokenVencidoNoCookie() throws Exception {
        String renovacao = valor(login(admin, null), "refreshToken");
        mvc.perform(post("/auth/refresh-token")
                .cookie(new Cookie("refreshToken", renovacao), new Cookie("accessToken", accessTokenVencido(admin))))
            .andExpect(status().isOk());
    }

    @Test
    void reusoDeTokenRotacionadoRevogaASessaoInteira() throws Exception {
        String original = valor(login(admin, null), "refreshToken");
        String novo = valor(refresh(original).andReturn(), "refreshToken");

        MvcResult reuso = refresh(original)
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_INVALID"))
            .andReturn();
        assertCookiesApagados(reuso);

        // A revogação por reuso é gravada apesar do 401 (noRollbackFor).
        assertThat(registro(novo).getMotivoRevogacao()).isEqualTo(MotivoRevogacao.REUTILIZADO);
        refresh(novo).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenVencidoDa401() throws Exception {
        String bruto = "vencido-" + UUID.randomUUID();
        RefreshTokenEntity t = new RefreshTokenEntity();
        t.setUsuario(usuarioRepository.findByUsername(admin).orElseThrow());
        t.setSessaoId(UUID.randomUUID());
        t.setTokenHash(RefreshTokenService.hash(bruto));
        t.setCriadoEm(Instant.now().minus(8, ChronoUnit.DAYS));
        t.setExpiraEm(Instant.now().minus(1, ChronoUnit.DAYS));
        refreshTokenRepository.save(t);

        assertCookiesApagados(refresh(bruto)
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_INVALID"))
            .andReturn());
    }

    @Test
    void refreshTokenDesconhecidoDa401() throws Exception {
        refresh("nao-existe-" + UUID.randomUUID())
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_INVALID"));
    }

    @Test
    void refreshSemCookieDa401EApagaOsCookies() throws Exception {
        assertCookiesApagados(mvc.perform(post("/auth/refresh-token"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_INVALID"))
            .andExpect(jsonPath("$.message").exists())
            .andReturn());
    }

    @Test
    void refreshesSimultaneosComOMesmoTokenSoUmVence() throws Exception {
        String original = valor(login(admin, null), "refreshToken");
        CyclicBarrier largada = new CyclicBarrier(2);
        Callable<Boolean> tentativa = () -> {
            largada.await(5, TimeUnit.SECONDS);
            try {
                refreshTokenService.rotacionar(original);
                return true;
            } catch (RefreshTokenInvalidoException e) {
                return false;
            }
        };

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> resultados = new ArrayList<>(pool.invokeAll(List.of(tentativa, tentativa)));
            long sucessos = 0;
            for (Future<Boolean> f : resultados) {
                if (f.get(10, TimeUnit.SECONDS)) sucessos++;
            }
            assertThat(sucessos).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
        UUID sessao = registro(original).getSessaoId();
        // Nunca sobram dois tokens ativos na mesma sessão.
        assertThat(refreshTokenRepository.findBySessaoId(sessao)).filteredOn(t -> t.getRevogadoEm() == null).hasSizeLessThanOrEqualTo(1);
    }

    // ---- logout ------------------------------------------------------------------------

    @Test
    void logoutRevogaASessaoEApagaOsCookies() throws Exception {
        String renovacao = valor(login(admin, null), "refreshToken");

        assertCookiesApagados(mvc.perform(post("/auth/logout").cookie(new Cookie("refreshToken", renovacao)))
            .andExpect(status().isOk()).andReturn());

        assertThat(registro(renovacao).getMotivoRevogacao()).isEqualTo(MotivoRevogacao.LOGOUT);
        refresh(renovacao).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutSemSessaoTambemResponde200() throws Exception {
        assertCookiesApagados(mvc.perform(post("/auth/logout")).andExpect(status().isOk()).andReturn());
    }

    // ---- contrato 401 / 403 ------------------------------------------------------------

    @Test
    void semTokenDa401AuthenticationRequired() throws Exception {
        mvc.perform(get("/equipe/retornarTodos"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void accessTokenVencidoDa401AccessTokenExpired() throws Exception {
        mvc.perform(get("/equipe/retornarTodos").cookie(new Cookie("accessToken", accessTokenVencido(admin))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("ACCESS_TOKEN_EXPIRED"))
            .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void accessTokenInvalidoDa401InvalidToken() throws Exception {
        mvc.perform(get("/equipe/retornarTodos").cookie(new Cookie("accessToken", "nao.e.jwt")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    void roleUserEmRotaDeAdminDa403AccessDenied() throws Exception {
        String acesso = valor(login(comum, null), "accessToken");
        mvc.perform(get("/equipe/retornarTodos").cookie(new Cookie("accessToken", acesso)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    // ---- apoio -------------------------------------------------------------------------

    private String criarUsuario(String papel) {
        String username = "teste-" + UUID.randomUUID();
        transacao.executeWithoutResult(s -> {
            PermissoesUsuarioEntity role = permissoesRepository.findByRole(papel).orElseGet(() -> {
                PermissoesUsuarioEntity r = new PermissoesUsuarioEntity();
                r.setRole(papel);
                return permissoesRepository.save(r);
            });
            usuarioRepository.save(new UsuarioEntity(username, passwordEncoder.encode(SENHA), Set.of(role)));
        });
        return username;
    }

    private MvcResult login(String username, String refreshAnterior) throws Exception {
        var req = post("/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"" + username + "\",\"password\":\"" + SENHA + "\"}");
        if (refreshAnterior != null) {
            req.cookie(new Cookie("refreshToken", refreshAnterior));
        }
        return mvc.perform(req).andExpect(status().isOk()).andReturn();
    }

    private org.springframework.test.web.servlet.ResultActions refresh(String refreshToken) throws Exception {
        return mvc.perform(post("/auth/refresh-token").cookie(new Cookie("refreshToken", refreshToken)));
    }

    private RefreshTokenEntity registro(String bruto) {
        return refreshTokenRepository.findByTokenHash(RefreshTokenService.hash(bruto)).orElseThrow();
    }

    private String accessTokenVencido(String username) {
        Instant emitido = Instant.now().minus(2, ChronoUnit.HOURS);
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("ficha-tecnica-auth-server")
            .issuedAt(emitido)
            .expiresAt(emitido.plus(20, ChronoUnit.MINUTES))
            .subject(username)
            .claim("roles", List.of("ROLE_ADMIN"))
            .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    private static String setCookie(MvcResult res, String nome) {
        return res.getResponse().getHeaders(HttpHeaders.SET_COOKIE).stream()
            .filter(h -> h.startsWith(nome + "="))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Set-Cookie " + nome + " ausente"));
    }

    private static String valor(MvcResult res, String nome) {
        String header = setCookie(res, nome);
        return header.substring(nome.length() + 1, header.indexOf(';'));
    }

    private static void assertCookiesApagados(MvcResult res) {
        for (String nome : List.of("accessToken", "refreshToken")) {
            assertThat(setCookie(res, nome)).startsWith(nome + "=;").contains("Max-Age=0", "HttpOnly", "SameSite=Strict", "Path=/");
        }
    }
}
