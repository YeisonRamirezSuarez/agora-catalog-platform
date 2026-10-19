package com.agora.catalog.common.handler;

import com.agora.catalog.common.dto.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    @DisplayName("Archivo demasiado grande: 400 con texto, no 500")
    void archivoDemasiadoGrande() throws NoSuchMethodException {
        ErrorResponse response = new GlobalExceptionHandler().handleMaxUploadSize(new MaxUploadSizeExceededException(5_242_880));

        assertThat(response.getMessage()).isEqualTo("El archivo supera el tamaño máximo permitido.");
        assertThat(GlobalExceptionHandler.class.getMethod("handleMaxUploadSize", MaxUploadSizeExceededException.class)
                .getAnnotation(ResponseStatus.class).value()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Cuerpo JSON ilegible: 400 con texto, no 500")
    void cuerpoIlegible() throws Exception {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error",
                new MockHttpInputMessage(new byte[0]));
        Method method = new ExceptionHandlerMethodResolver(GlobalExceptionHandler.class).resolveMethod(ex);

        assertThat(method.getAnnotation(ResponseStatus.class).value()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse response = (ErrorResponse) method.invoke(new GlobalExceptionHandler(), ex);
        assertThat(response.getError()).isEqualTo("Solicitud inválida");
        assertThat(response.getMessage()).isEqualTo("El cuerpo de la petición no tiene un formato válido.");
    }
}
