package com.example.tdd.domain.service;

import com.example.tdd.domain.enums.TransactionStatus;
import com.example.tdd.domain.model.TransactionHistory;
import com.example.tdd.domain.repository.TransactionHistoryRepository;
import com.example.tdd.domain.repository.TransactionRepository;
import com.example.tdd.shared.exception.ResourceNotFoundException;
import com.example.tdd.shared.exception.TransactionProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Importante para R2DBC transaccional
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private static final String USER_SYSTEM = "SYSTEM"; // Usuario por defecto para este ejemplo

    @Transactional // Asegura la atomicidad de las operaciones de base de datos
    public Mono<TransactionHistory> cancelTransaction(Long transactionId) {
        log.info("Attempting to cancel transaction with ID: {}", transactionId);

        return transactionRepository.findById(transactionId)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Transaction not found with id: " + transactionId)))
            .flatMap(transaction -> {
                if (transaction.getStatus() == TransactionStatus.CANCELLED) {
                    log.warn("Transaction {} is already cancelled.", transactionId);
                    return Mono.error(new TransactionProcessingException("Transaction already cancelled."));
                }

                TransactionStatus previousStatus = transaction.getStatus();
                transaction.setStatus(TransactionStatus.CANCELLED);

                // Crear el registro de historial
                TransactionHistory historyRecord = TransactionHistory.builder()
                    .transactionId(transactionId)
                    .previousStatus(previousStatus)
                    .newStatus(TransactionStatus.CANCELLED)
                    .changeDate(LocalDateTime.now())
                    .userPerformingAction(USER_SYSTEM) // En un caso real, obtener del contexto de seguridad
                    .build();

                // Guardar el historial y luego actualizar la transacción
                // El orden podría ser actualizar y luego guardar historial también.
                // @Transactional se encargará del rollback si alguna operación falla.
                return transactionHistoryRepository.save(historyRecord)
                    .doOnSuccess(savedHistory -> log.info("History record created for transaction {}: {}", transactionId, savedHistory.getId()))
                    .doOnError(e -> log.error("Failed to save transaction history for transaction ID: {}", transactionId, e))
                    .then(transactionRepository.updateStatus(transactionId, TransactionStatus.CANCELLED))
                    .doOnSuccess(updateCount -> {
                        if (updateCount > 0) {
                            log.info("Transaction {} status updated to CANCELLED.", transactionId);
                        } else {
                            // Esto no debería ocurrir si findById tuvo éxito y la transacción existe.
                            log.error("Failed to update status for transaction ID: {}. Update count was {}.", transactionId, updateCount);
                            // Se podría lanzar una excepción específica aquí si updateCount es 0 después de un find exitoso.
                        }
                    })
                    .doOnError(e -> log.error("Failed to update transaction status for transaction ID: {}", transactionId, e))
                    .thenReturn(historyRecord) // Devolvemos el registro de historial como confirmación
                    .onErrorResume(ex -> {
                         log.error("Error during transaction cancellation for ID {}: {}", transactionId, ex.getMessage());
                         // No es necesario hacer rollback manual aquí, @Transactional se encarga.
                         // Simplemente propagamos un error que indique la falla de la operación.
                         return Mono.error(new TransactionProcessingException("Failed to cancel transaction: " + ex.getMessage(), ex));
                     });
            });
    }
}