package com.example.tdd.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import com.example.tdd.domain.enums.TransactionStatus;

import java.math.BigDecimal;

@Data
@NoArgsConstructor 
@AllArgsConstructor
@Table("transaction") // Nombre de la tabla en la base de datos
public class Transaction {

    @Id
    private Long id;
    private TransactionStatus status;
    private BigDecimal amount; // Campo de ejemplo
    private String currency; // Campo de ejemplo
}
