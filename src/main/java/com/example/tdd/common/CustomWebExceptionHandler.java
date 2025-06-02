package com.example.tdd.common;

import com.example.tdd.presentation.dto.ErrorResponse;
import com.example.tdd.shared.exception.InvalidInputException;
import com.example.tdd.shared.exception.MissingHeaderException;
import com.example.tdd.shared.exception.ResourceNotFoundException;
import com.example.tdd.shared.exception.TransactionProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@Order(-2) // Debe tener mayor precedencia que el DefaultErrorWebExceptionHandler
@Slf4j
public class CustomWebExceptionHandler extends AbstractErrorWebExceptionHandler {

    public CustomWebExceptionHandler(ErrorAttributes errorAttributes,
                                     ApplicationContext applicationContext,
                                     ServerCodecConfigurer serverCodecConfigurer) {
        super(errorAttributes, new WebProperties.Resources(), applicationContext);
        super.setMessageWriters(serverCodecConfigurer.getWriters());
        super.setMessageReaders(serverCodecConfigurer.getReaders());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Throwable error = getError(request);
        log.error("Handling error: {} for request {}", error.getMessage(), request.path(), error);

        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR; // Default
        String message = "An unexpected error occurred.";

        if (error instanceof MissingHeaderException || error instanceof InvalidInputException) {
            httpStatus = HttpStatus.BAD_REQUEST;
            message = error.getMessage();
        } else if (error instanceof ResourceNotFoundException) {
            httpStatus = HttpStatus.NOT_FOUND;
            message = error.getMessage();
        } else if (error instanceof TransactionProcessingException) {
            httpStatus = HttpStatus.CONFLICT; // O podría ser UNPROCESSABLE_ENTITY (422)
            message = error.getMessage();
        }
        // Puedes añadir más mapeos de excepciones aquí

        ErrorResponse errorResponse = new ErrorResponse(
                message,
                request.path(),
                httpStatus.value(),
                LocalDateTime.now()
        );

        return ServerResponse.status(httpStatus)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(errorResponse));
    }
}