package com.example.tdd.unit.service;

import com.example.tdd.domain.enums.TransactionStatus;
import com.example.tdd.domain.model.Transaction;
import com.example.tdd.domain.model.TransactionHistory;
import com.example.tdd.domain.repository.TransactionHistoryRepository;
import com.example.tdd.domain.repository.TransactionRepository;
import com.example.tdd.domain.service.TransactionService;
import com.example.tdd.shared.exception.ResourceNotFoundException;
import com.example.tdd.shared.exception.TransactionProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionHistoryRepository transactionHistoryRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void cancelTransaction_shouldSucceed_whenTransactionExistsAndNotCancelled() {
        Transaction transaction = new Transaction(1L, TransactionStatus.PENDING, null, null);
        TransactionHistory history = TransactionHistory.builder().transactionId(1L).build(); // Simulación

        when(transactionRepository.findById(1L)).thenReturn(Mono.just(transaction));
        when(transactionHistoryRepository.save(any(TransactionHistory.class))).thenReturn(Mono.just(history));
        when(transactionRepository.updateStatus(1L, TransactionStatus.CANCELLED)).thenReturn(Mono.just(1)); // 1 row updated

        StepVerifier.create(transactionService.cancelTransaction(1L))
                .expectNextMatches(savedHistory -> {
                    // Aquí podrías hacer assertions más detalladas sobre el objeto 'savedHistory'
                    return savedHistory.getTransactionId().equals(1L) &&
                           savedHistory.getNewStatus().equals(TransactionStatus.CANCELLED);
                })
                .verifyComplete();

        verify(transactionRepository).findById(1L);
        verify(transactionHistoryRepository).save(any(TransactionHistory.class));
        verify(transactionRepository).updateStatus(1L, TransactionStatus.CANCELLED);
    }

    @Test
    void cancelTransaction_shouldFail_whenTransactionNotFound() {
        when(transactionRepository.findById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(transactionService.cancelTransaction(1L))
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(transactionRepository).findById(1L);
        verifyNoInteractions(transactionHistoryRepository); // No debe intentar guardar historial
    }

    @Test
    void cancelTransaction_shouldFail_whenTransactionAlreadyCancelled() {
        Transaction transaction = new Transaction(1L, TransactionStatus.CANCELLED, null, null);
        when(transactionRepository.findById(1L)).thenReturn(Mono.just(transaction));

        StepVerifier.create(transactionService.cancelTransaction(1L))
                .expectErrorMatches(throwable -> throwable instanceof TransactionProcessingException &&
                                                throwable.getMessage().contains("Transaction already cancelled"))
                .verify();

        verify(transactionRepository).findById(1L);
        verifyNoInteractions(transactionHistoryRepository);
    }

    @Test
    void cancelTransaction_shouldRollback_whenHistorySaveFails() {
        Transaction transaction = new Transaction(1L, TransactionStatus.PENDING, null, null);

        when(transactionRepository.findById(1L)).thenReturn(Mono.just(transaction));
        when(transactionHistoryRepository.save(any(TransactionHistory.class)))
            .thenReturn(Mono.error(new RuntimeException("DB error on history save"))); // Simula fallo

        StepVerifier.create(transactionService.cancelTransaction(1L))
            .expectErrorMatches(throwable -> throwable instanceof TransactionProcessingException &&
                                            throwable.getCause().getMessage().contains("DB error on history save"))
            .verify();

        verify(transactionRepository).findById(1L);
        verify(transactionHistoryRepository).save(any(TransactionHistory.class));
        // En un test unitario, no podemos verificar directamente el rollback de la DB.
        // Lo que verificamos es que no se llamó a `transactionRepository.updateStatus` si el save de historial falló ANTES.
        // Si @Transactional está funcionando, Spring se encarga del rollback.
        // Para este flujo específico (save history THEN update status), la verificación de no llamar a updateStatus
        // si history save falla es una buena indicación.
        verify(transactionRepository, never()).updateStatus(anyLong(), any(TransactionStatus.class));
    }

    @Test
    void cancelTransaction_shouldRollback_whenTransactionUpdateFails() {
        Transaction transaction = new Transaction(1L, TransactionStatus.PENDING, null, null);
        TransactionHistory history = TransactionHistory.builder().transactionId(1L).build();

        when(transactionRepository.findById(1L)).thenReturn(Mono.just(transaction));
        when(transactionHistoryRepository.save(any(TransactionHistory.class))).thenReturn(Mono.just(history));
        when(transactionRepository.updateStatus(1L, TransactionStatus.CANCELLED))
            .thenReturn(Mono.error(new RuntimeException("DB error on transaction update"))); // Simula fallo

        StepVerifier.create(transactionService.cancelTransaction(1L))
                .expectErrorMatches(throwable -> throwable instanceof TransactionProcessingException &&
                                            throwable.getCause().getMessage().contains("DB error on transaction update"))
                .verify();

        verify(transactionRepository).findById(1L);
        verify(transactionHistoryRepository).save(any(TransactionHistory.class));
        verify(transactionRepository).updateStatus(1L, TransactionStatus.CANCELLED);
        // La anotación @Transactional debería manejar el rollback. En un test unitario,
        // es difícil verificar el rollback de la base de datos directamente.
        // Lo que probamos es que la excepción se propaga correctamente.
    }
}