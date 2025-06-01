package com.example.tdd.presentation.router;

import com.example.tdd.presentation.handler.TransactionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class TransactionRouter {

    private static final String TRANSACTIONS_BASE_PATH = "/transacciones";

    @Bean
    public RouterFunction<ServerResponse> transactionRoutes(TransactionHandler transactionHandler) {
        return route()
                .nest(accept(MediaType.APPLICATION_JSON), builder -> builder
                    .PATCH(TRANSACTIONS_BASE_PATH + "/{id}/cancelar", transactionHandler::cancelTransaction)
                ).build();
    }
}