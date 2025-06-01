package com.example.tdd.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import com.example.tdd.domain.enums.TransactionStatus;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("transaction_history") // Nombre de la tabla en la base de datos
public class TransactionHistory {

    @Id
    private Long id;

    @Column("transaction_id")
    private Long transactionId;

    @Column("previous_status")
    private TransactionStatus previousStatus;

    @Column("new_status")
    private TransactionStatus newStatus;

    @Column("change_date")
    private LocalDateTime changeDate;

    @Column("user_performing_action")
    private String userPerformingAction;
}
