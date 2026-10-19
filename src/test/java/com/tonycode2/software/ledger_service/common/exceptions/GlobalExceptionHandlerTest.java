package com.tonycode2.software.ledger_service.common.exceptions;

import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class GlobalExceptionHandlerTest {
        private static @Nullable Object OptimisticLockingFailureException;
        private ThrowingController controller;
        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                controller = new ThrowingController();
                mockMvc = MockMvcBuilders
                                .standaloneSetup(controller)
                                .setControllerAdvice(new GlobalExceptionHandler())
                                .build();
        }

        // ---------- Controller falso y DTO de prueba ----------

        @RestController
        static class ThrowingController {
                RuntimeException toThrow;

                @GetMapping("/test/throw")
                String throwIt() {
                        throw toThrow;
                }

                @PostMapping("/test/validate")
                String validate(@Valid @RequestBody SampleRequest request) {
                        return "ok";
                }
        }

        record SampleRequest(
                        @NotBlank String owner,
                        @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency) {
        }

        // ---------- Excepciones de dominio ----------

        /*
         * InsufficientFundsException e InvalidAmountException usan sus constructores
         * reales. InvalidAccount e InvalidEntry se asumen con String: ajustalos si
         * reciben otros parametros.
         */
        static Stream<Arguments> domainExceptions() {
                return Stream.of(
                                Arguments.of(new InsufficientFundsException(UUID.randomUUID(), 300L, 500L), 422,
                                                "Insufficient Funds"),
                                Arguments.of(new InvalidAmountException(-1L), 400, "Invalid Amount"),
                                Arguments.of(new InvalidAccountException("Owner is required"), 400, "Invalid Account"),
                                Arguments.of(new AccountNotFoundException(UUID.randomUUID()), 404, "Account Not Found"),
                                Arguments.of(new InvalidLedgerTransactionException("Type must not be null"), 400,
                                                "Invalid Ledger Transaction"),
                                Arguments.of(new InvalidEntryException("Amount must be positive"), 400,
                                                "Invalid Entry"),
                                Arguments.of(new IdempotencyKeyReusedException("Key already used with other content"),
                                                422,
                                                "Invalid hash with current idempotency key"),
                                Arguments.of(new InvalidCurrencyException("Currency mismatch"), 422,
                                                "Invalid Currency"));
        }

        @ParameterizedTest
        @MethodSource("domainExceptions")
        void domainException_isMappedToExpectedProblemDetail(RuntimeException ex, int expectedStatus,
                        String expectedTitle) throws Exception {
                controller.toThrow = ex;

                mockMvc.perform(get("/test/throw"))
                                .andExpect(status().is(expectedStatus))
                                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                                .andExpect(jsonPath("$.status").value(expectedStatus))
                                .andExpect(jsonPath("$.title").value(expectedTitle))
                                .andExpect(jsonPath("$.detail").value(ex.getMessage()));
        }

        // ---------- Error inesperado ----------

        @Test
        void unexpectedException_returns500WithoutLeakingInternalMessage() throws Exception {
                controller.toThrow = new IllegalStateException("db password is hunter2");

                mockMvc.perform(get("/test/throw"))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.title").value("Unexpected Error"))
                                .andExpect(jsonPath("$.detail").value("Unexpected Error"))
                                .andExpect(content().string(not(containsString("hunter2"))));
        }

        // ---------- Validacion de DTOs ----------

        @Test
        void invalidBody_returnsOneErrorPerInvalidField() throws Exception {
                String body = "{\"owner\":\"\",\"currency\":\"US\"}";

                mockMvc.perform(post("/test/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.title").value("Validation Failed"))
                                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                                .andExpect(jsonPath("$.errors", hasSize(2)))
                                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("owner", "currency")));
        }

        @Test
        void validBody_passesValidation() throws Exception {
                String body = "{\"owner\":\"Ana\",\"currency\":\"USD\"}";

                mockMvc.perform(post("/test/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                .andExpect(status().isOk());
        }

        // ---------- Errores de Spring MVC que NO deben caer en el handler generico
        // ----------

        @Test
        void malformedJson_returns400NotUnexpectedError() throws Exception {
                mockMvc.perform(post("/test/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{bad"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void missingBody_returns400() throws Exception {
                mockMvc.perform(post("/test/validate")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest());
        }

}
