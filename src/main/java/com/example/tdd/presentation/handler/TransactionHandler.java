package com.example.tdd.presentation.handler;

import com.example.tdd.domain.service.TransactionService;
import com.example.tdd.shared.exception.InvalidInputException;
import com.example.tdd.shared.exception.MissingHeaderException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionHandler {

    private final TransactionService transactionService;
    private static final String ID_MENSAJE_HEADER = "idMensaje";

    public Mono<ServerResponse> cancelTransaction(ServerRequest request) {
        // 1. Validar header 'idMensaje'
        String idMensaje = request.headers().firstHeader(ID_MENSAJE_HEADER);
        if (idMensaje == null || idMensaje.isBlank()) {
            log.warn("Missing or empty header: {}", ID_MENSAJE_HEADER);
            return Mono.error(new MissingHeaderException("Header '" + ID_MENSAJE_HEADER + "' is mandatory."));
        }
        log.info("Received request with {}: {}", ID_MENSAJE_HEADER, idMensaje);

        // 2. Validar ID de transacción del path
        String idParam = request.pathVariable("id");
        Long transactionId;
        try {
            transactionId = Long.parseLong(idParam);
            if (transactionId <= 0) {
                log.warn("Invalid transaction ID: {}. Must be greater than 0.", idParam);
                return Mono.error(new InvalidInputException("Transaction ID must be a positive number."));
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid transaction ID format: {}", idParam);
            return Mono.error(new InvalidInputException("Transaction ID must be a numeric value."));
        }
        log.info("Validated transaction ID: {}", transactionId);

        // 3. Llamar al servicio de dominio
        return transactionService.cancelTransaction(transactionId)
                .flatMap(history -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(history)) // Devuelve el registro de historial creado
                .doOnSuccess(response -> log.info("Transaction {} cancelled successfully.", transactionId))
                .doOnError(error -> log.error("Error processing cancel request for transaction {}: {}", transactionId, error.getMessage()));
                // El manejo de errores específico para HTTP se hará en CustomWebExceptionHandler
    }
}