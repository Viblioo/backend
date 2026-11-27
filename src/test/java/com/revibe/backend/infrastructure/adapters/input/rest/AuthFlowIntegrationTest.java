package com.revibe.backend.infrastructure.adapters.input.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.revibe.backend.application.ports.output.security.PasswordHasherPort;
import com.revibe.backend.application.ports.output.security.RateLimit;
import com.revibe.backend.application.ports.output.security.RateLimiterPort;
import com.revibe.backend.application.ports.output.security.SessionPort;
import com.revibe.backend.application.ports.output.user.UserRepositoryPort;
import com.revibe.backend.domain.model.User;
import com.revibe.backend.domain.model.UserStatus;
import com.revibe.backend.infrastructure.adapters.output.persistence.postgres.repository.UserJpaRepository;
import com.revibe.backend.support.IntegrationTest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

/**
 * Flujos completos por HTTP (seguridad incluida) contra la BD y el Redis reales. Cada test usa un usuario y una IP
 * aleatorios y lo borra todo al acabar.
 */
class AuthFlowIntegrationTest extends IntegrationTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String PASSWORD = "Secreta123!";
    private static final String NEW_PASSWORD = "OtraClave456!";

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private UserRepositoryPort users;
    @Autowired
    private UserJpaRepository userJpa;
    @Autowired
    private PasswordHasherPort hasher;
    @Autowired
    private SessionPort sessions;
    @Autowired
    private RateLimiterPort limiter;
    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private String ip;
    private String username;
    private String email;
    private final List<UUID> createdUsers = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        Mockito.reset(emailSender);
        var random = ThreadLocalRandom.current();
        ip = "10.%d.%d.%d".formatted(random.nextInt(256), random.nextInt(256), random.nextInt(1, 255));
        String suffix = HexFormat.of().formatHex(UUID.randomUUID().toString().getBytes()).substring(0, 10);
        username = "t_" + suffix;
        email = suffix + "@example.com";
    }

    @AfterEach
    void cleanUp() {
        for (UUID id : createdUsers) {
            sessions.revokeAll(id);
            limiter.reset(RateLimit.LOGIN_FAILURES_PER_ACCOUNT, id.toString());
            limiter.reset(RateLimit.CHANGE_PASSWORD_FAILURES_PER_USER, id.toString());
            jdbc.update("DELETE FROM graph_outbox WHERE payload->>'userId' = ?", id.toString());
            userJpa.deleteById(id);
        }
        limiter.reset(RateLimit.LOGIN_FAILURES_PER_IP, ip);
        limiter.reset(RateLimit.REGISTER_PER_IP, ip);
        limiter.reset(RateLimit.EMAIL_REQUESTS_PER_IP, ip);
        limiter.reset(RateLimit.EMAIL_REQUESTS_PER_ADDRESS, "activation:" + email);
        limiter.reset(RateLimit.EMAIL_REQUESTS_PER_ADDRESS, "reset:" + email);
    }

    // ---------------------------------------------------------------- tests

    @Test
    void registerResendActivateAndLogIn() throws Exception {
        perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"%s","email":"%s","password":"%s","firstName":"Test","birthDate":"2000-05-14"}
                """.formatted(username, email, PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("pending"));
        var userId = users.findByUsername(username).orElseThrow().getId();
        createdUsers.add(userId);
        verify(emailSender, timeout(5000)).sendActivation(eq(email), anyString());

        // Sin activar: 403 con status pending
        login(username, PASSWORD).andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value("pending"));

        // Reenviar: llega un segundo email con otro código
        perform(post("/api/v1/auth/activation/resend").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\"}".formatted(email)))
                .andExpect(status().isAccepted());
        var codes = ArgumentCaptor.forClass(String.class);
        verify(emailSender, timeout(5000).times(2)).sendActivation(eq(email), codes.capture());
        String code = codes.getAllValues().getLast();

        perform(get("/api/v1/auth/activate").param("code", code))
                .andExpect(status().isSeeOther())
                .andExpect(header().string(HttpHeaders.LOCATION, "/activation/success.html"));
        // El código es de un solo uso
        perform(get("/api/v1/auth/activate").param("code", code))
                .andExpect(header().string(HttpHeaders.LOCATION, "/activation/error.html"));

        assertThat(users.findById(userId).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
        Integer outboxRows = jdbc.queryForObject(
                "SELECT count(*) FROM graph_outbox WHERE payload->>'userId' = ?", Integer.class, userId.toString());
        assertThat(outboxRows).isEqualTo(1);

        login(username, PASSWORD).andExpect(status().isOk());
    }

    @Test
    void loginMeAndLogout() throws Exception {
        activeUser();

        String byUsername = token(login(username, PASSWORD));
        String byEmail = token(login(email.toUpperCase(), PASSWORD));

        perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + byUsername))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        perform(post("/api/v1/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + byUsername))
                .andExpect(status().isNoContent());

        me(byUsername).andExpect(status().isUnauthorized());
        me(byEmail).andExpect(status().isOk()); // la otra sesión sigue abierta
        perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordIs401AndAfterFiveFailuresEvenTheRightOneIs429() throws Exception {
        activeUser();

        for (int i = 0; i < RateLimit.LOGIN_FAILURES_PER_ACCOUNT.maxAttempts(); i++) {
            login(username, "mala-" + i).andExpect(status().isUnauthorized());
        }

        login(username, PASSWORD)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.retryAfterSeconds").isNumber());
        // Entrar con el email cuenta para el mismo bloqueo
        login(email, PASSWORD).andExpect(status().isTooManyRequests());
    }

    @Test
    void changePasswordKeepsThisSessionAndClosesTheOthers() throws Exception {
        activeUser();
        String current = token(login(username, PASSWORD));
        String otherDevice = token(login(username, PASSWORD));

        changePassword(current, "incorrecta", NEW_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.currentPassword").exists());

        changePassword(current, PASSWORD, NEW_PASSWORD).andExpect(status().isNoContent());

        me(current).andExpect(status().isOk());
        me(otherDevice).andExpect(status().isUnauthorized());
        login(username, PASSWORD).andExpect(status().isUnauthorized());
        login(username, NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void passwordResetWithTheEmailedCodeClosesAllSessions() throws Exception {
        activeUser();
        String session = token(login(username, PASSWORD));

        requestReset(email).andExpect(status().isAccepted());
        var captured = ArgumentCaptor.forClass(String.class);
        verify(emailSender, timeout(5000)).sendPasswordReset(eq(email), captured.capture());
        String code = captured.getValue();
        String wrongCode = code.equals("000000") ? "111111" : "000000";

        confirmReset(wrongCode, NEW_PASSWORD).andExpect(status().isBadRequest());
        confirmReset(code, NEW_PASSWORD).andExpect(status().isNoContent());
        confirmReset(code, NEW_PASSWORD).andExpect(status().isBadRequest()); // ya usado

        me(session).andExpect(status().isUnauthorized());
        login(username, PASSWORD).andExpect(status().isUnauthorized());
        login(username, NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void passwordResetForUnknownEmailLooksTheSameButSendsNothing() throws Exception {
        requestReset("nadie-" + email).andExpect(status().isAccepted());

        verify(emailSender, after(1000).never()).sendPasswordReset(anyString(), anyString());
        limiter.reset(RateLimit.EMAIL_REQUESTS_PER_ADDRESS, "reset:nadie-" + email);
    }

    @Test
    void protectedEndpointsNeedAValidToken() throws Exception {
        me("token-inventado").andExpect(status().isUnauthorized());
        perform(post("/api/v1/auth/logout")).andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- helpers

    /** Todas las peticiones salen desde la IP aleatoria del test, para no tocar los contadores de 127.0.0.1. */
    private ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.with(r -> {
            r.setRemoteAddr(ip);
            return r;
        }));
    }

    private void activeUser() {
        var saved = users.save(new User(null, username, email, hasher.hash(PASSWORD), "Test", null,
                LocalDate.of(2000, 5, 14), UserStatus.ACTIVE, null));
        createdUsers.add(saved.getId());
    }

    private ResultActions login(String login, String password) throws Exception {
        return perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(login, password)));
    }

    private ResultActions me(String token) throws Exception {
        return perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private ResultActions changePassword(String token, String current, String newPassword) throws Exception {
        return perform(post("/api/v1/users/me/password")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"%s\",\"newPassword\":\"%s\"}".formatted(current, newPassword)));
    }

    private ResultActions requestReset(String address) throws Exception {
        return perform(post("/api/v1/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\"}".formatted(address)));
    }

    private ResultActions confirmReset(String code, String newPassword) throws Exception {
        return perform(post("/api/v1/auth/password-reset/confirm").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"code\":\"%s\",\"newPassword\":\"%s\"}"
                        .formatted(email, code, newPassword)));
    }

    @SuppressWarnings("unchecked")
    private static String token(ResultActions login) throws Exception {
        String body = login.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return (String) JSON.readValue(body, Map.class).get("token");
    }
}
