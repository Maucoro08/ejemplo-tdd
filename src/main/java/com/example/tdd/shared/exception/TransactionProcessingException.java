package com.example.tdd.shared.exception;

public class TransactionProcessingException extends RuntimeException {
    public TransactionProcessingException(String message) {
        super(message);
    }
    public TransactionProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}