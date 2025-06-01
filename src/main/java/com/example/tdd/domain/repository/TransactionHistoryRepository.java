package com.example.tdd.domain.repository;

import com.example.tdd.domain.model.TransactionHistory;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface TransactionHistoryRepository extends ReactiveCrudRepository<TransactionHistory, Long> {
}
