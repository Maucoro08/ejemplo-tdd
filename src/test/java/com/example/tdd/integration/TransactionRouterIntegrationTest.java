package com.example.tdd.integration;

import com.example.tdd.TddApplication;
import com.example.tdd.domain.enums.TransactionStatus;
import com.example.tdd.domain.model.Transaction;
import com.example.tdd.domain.model.TransactionHistory;
import com.example.tdd.domain.repository.TransactionHistoryRepository;
import com.example.tdd.domain.repository.TransactionRepository;
import com.example.tdd.presentation.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = TddApplication.class)
@AutoConfigureWebTestClient
@ActiveProfiles("test") // Si tuvieras perfiles específicos para test
public class TransactionRouterIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionHistoryRepository transactionHistoryRepository;

    private static final String BASE_PATH = "/transacciones";
    private static final String USER_SYSTEM = "SYSTEM";

    @BeforeEach
    void setUp() {
        // Limpiar datos antes de cada test para asegurar la idempotencia
        transactionHistoryRepository.deleteAll().block();
        transactionRepository.deleteAll().block();
    }

    private Transaction createAndSaveTransaction(TransactionStatus status) {
        return transactionRepository.save(new Transaction(null, status, BigDecimal.valueOf(100.00), "USD")).block();
    }

    @Test
    void cancelTransaction_whenHeaderMissing_shouldReturnBadRequest() {
        webTestClient.patch()
                .uri(BASE_PATH + "/1/cancelar")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(error -> assertTrue(error.message().contains("Header 'idMensaje' is mandatory")));
    }

    @Test
    void cancelTransaction_whenTransactionIdIsInvalid_shouldReturnBadRequest() {
        webTestClient.patch()
                .uri(BASE_PATH + "/0/cancelar")
                .header("idMensaje", "msg-123")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(error -> assertTrue(error.message().contains("Transaction ID must be a positive number")));

        webTestClient.patch()
                .uri(BASE_PATH + "/abc/cancelar")
                .header("idMensaje", "msg-123")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(error -> assertTrue(error.message().contains("Transaction ID must be a numeric value")));
    }

    @Test
    void cancelTransaction_whenTransactionNotFound_shouldReturnNotFound() {
        webTestClient.patch()
                .uri(BASE_PATH + "/999/cancelar")
                .header("idMensaje", "msg-123")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(error -> assertTrue(error.message().contains("Transaction not found with id: 999")));
    }

    @Test
    void cancelTransaction_whenTransactionAlreadyCancelled_shouldReturnConflict() {
        Transaction transaction = createAndSaveTransaction(TransactionStatus.CANCELLED);

        webTestClient.patch()
                .uri(BASE_PATH + "/" + transaction.getId() + "/cancelar")
                .header("idMensaje", "msg-123")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT) // O 422 si se prefiere
                .expectBody(ErrorResponse.class)
                .value(error -> assertTrue(error.message().contains("Transaction already cancelled")));
    }

    @Test
    void cancelTransaction_whenSuccessful_shouldReturnOkAndCreateHistory() {
        Transaction transaction = createAndSaveTransaction(TransactionStatus.PENDING);
        Long transactionId = transaction.getId();

        webTestClient.patch()
                .uri(BASE_PATH + "/" + transactionId + "/cancelar")
                .header("idMensaje", "msg-123")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(TransactionHistory.class)
                .value(history -> {
                    assertNotNull(history.getId());
                    assertEquals(transactionId, history.getTransactionId());
                    assertEquals(TransactionStatus.PENDING, history.getPreviousStatus());
                    assertEquals(TransactionStatus.CANCELLED, history.getNewStatus());
                    assertEquals(USER_SYSTEM, history.getUserPerformingAction());
                    assertNotNull(history.getChangeDate());
                });

        // Verificar en la base de datos
        StepVerifier.create(transactionRepository.findById(transactionId))
                .expectNextMatches(updatedTx -> updatedTx.getStatus() == TransactionStatus.CANCELLED)
                .verifyComplete();

        StepVerifier.create(transactionHistoryRepository.findAll().filter(h -> h.getTransactionId().equals(transactionId)))
                .expectNextCount(1) // Debería haber un solo registro de historial para esta transacción
                .verifyComplete();
    }

    // Test de atomicidad: Este es más complejo de probar en un entorno de integración sin
    // introducir puntos de fallo artificiales en el código o mocks a nivel de infraestructura.
    // Con @Transactional, confiamos en que Spring/R2DBC manejan el rollback.
    // Una forma simplificada de pensar en esto es verificar que si una parte falla (ej. constraint en DB),
    // la otra no se complete. Aquí no tenemos un constraint fácil de violar en el save de historial
    // que no sea un error de conectividad (difícil de simular limpiamente).
    // Para este ejemplo, la prueba de éxito y las pruebas unitarias del servicio cubren
    // razonablemente el comportamiento esperado.
}