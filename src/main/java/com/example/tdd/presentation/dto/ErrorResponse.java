package com.example.tdd.presentation.dto;

import java.time.LocalDateTime;

public record ErrorResponse(String message, String path, int statusCode, LocalDateTime timestamp) {}