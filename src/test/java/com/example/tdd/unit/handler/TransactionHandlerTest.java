package com.example.tdd.unit.handler;

import com.example.tdd.domain.model.TransactionHistory;
import com.example.tdd.domain.service.TransactionService;
import com.example.tdd.presentation.handler.TransactionHandler;
import com.example.tdd.shared.exception.InvalidInputException;
import com.example.tdd.shared.exception.MissingHeaderException;
import com.example.tdd.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(MockitoExtension.class)
class TransactionHandlerTest {

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransactionHandler transactionHandler;

    private MockServerRequest.Builder requestBuilder;

    @BeforeEach
    void setUp() {
        requestBuilder = MockServerRequest.builder()
                .pathVariable("id", "1");
    }

    @Test
    void cancelTransaction_shouldFail_whenIdMensajeHeaderIsMissing() {
        MockServerRequest request = requestBuilder.build();

        StepVerifier.create(transactionHandler.cancelTransaction(request))
            .expectErrorMatches(throwable -> throwable instanceof MissingHeaderException &&
                                            throwable.getMessage().contains("Header 'idMensaje' is mandatory"))
            .verify();
    }

    @Test
    void cancelTransaction_shouldFail_whenTransactionIdIsZero() {
        MockServerRequest request = MockServerRequest.builder()
                .header("idMensaje", "test-msg-id")
                .pathVariable("id", "0")
                .build();

        StepVerifier.create(transactionHandler.cancelTransaction(request))
            .expectErrorMatches(throwable -> throwable instanceof InvalidInputException &&
                                            throwable.getMessage().contains("Transaction ID must be a positive number"))
            .verify();
    }

    @Test
    void cancelTransaction_shouldFail_whenTransactionIdIsNullString() {
         MockServerRequest request = MockServerRequest.builder()
                .header("idMensaje", "test-msg-id")
                .pathVariable("id", "null") // Esto causará NumberFormatException
                .build();

        StepVerifier.create(transactionHandler.cancelTransaction(request))
            .expectErrorMatches(throwable -> throwable instanceof InvalidInputException &&
                                            throwable.getMessage().contains("Transaction ID must be a numeric value"))
            .verify();
    }
    
    @Test
    void cancelTransaction_shouldFail_whenTransactionIdIsNotNumeric() {
        MockServerRequest request = MockServerRequest.builder()
                .header("idMensaje", "test-msg-id")
                .pathVariable("id", "abc")
                .build();

        StepVerifier.create(transactionHandler.cancelTransaction(request))
            .expectErrorMatches(throwable -> throwable instanceof InvalidInputException &&
                                            throwable.getMessage().contains("Transaction ID must be a numeric value"))
            .verify();
    }


    @Test
    void cancelTransaction_shouldSucceed_whenInputIsValid() {
        MockServerRequest request = requestBuilder.header("idMensaje", "test-msg-id").build();
        TransactionHistory mockHistory = TransactionHistory.builder().id(1L).transactionId(1L).build();

        when(transactionService.cancelTransaction(1L)).thenReturn(Mono.just(mockHistory));

        StepVerifier.create(transactionHandler.cancelTransaction(request))
                .consumeNextWith(response -> {
                    assertEquals(HttpStatus.OK, response.statusCode());
                    // Podrías verificar el cuerpo aquí si es necesario, usando StepVerifier para extraer el Mono<EntityResponse>
                })
                .verifyComplete();
    }

    @Test
    void cancelTransaction_shouldPropagateServiceError() {
        MockServerRequest request = requestBuilder.header("idMensaje", "test-msg-id").build();
        when(transactionService.cancelTransaction(1L)).thenReturn(Mono.error(new ResourceNotFoundException("Not found")));

        StepVerifier.create(transactionHandler.cancelTransaction(request))
            .expectError(ResourceNotFoundException.class)
            .verify();
    }
}