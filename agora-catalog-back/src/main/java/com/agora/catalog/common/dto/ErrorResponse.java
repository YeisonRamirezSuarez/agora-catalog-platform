package com.agora.catalog.common.dto;

import lombok.Getter;

/** Cuerpo de todo error de la API: un título corto y un mensaje para mostrar tal cual. */
@Getter
public class ErrorResponse {

    private final String error;
    private final String message;

    private ErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
    }

    public static ErrorResponse of(String error, String message) {
        return new ErrorResponse(error, message);
    }

}
