package com.example.tdd.domain.repository;

import com.example.tdd.domain.model.Transaction;
import com.example.tdd.domain.enums.TransactionStatus;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface TransactionRepository extends ReactiveCrudRepository<Transaction, Long> {

    @Modifying // Indica que esta query modifica datos
    @Query("UPDATE transaction SET status = :newStatus WHERE id = :id")
    Mono<Integer> updateStatus(@Param("id") Long id, @Param("newStatus") TransactionStatus newStatus);
}
