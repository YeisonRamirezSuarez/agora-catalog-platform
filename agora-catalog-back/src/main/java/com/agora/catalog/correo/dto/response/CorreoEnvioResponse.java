package com.agora.catalog.correo.dto.response;

import java.time.OffsetDateTime;

/** Una fila de la bitácora de envíos. */
public record CorreoEnvioResponse(
        long id,
        Long empresaId,
        String destinatario,
        String asunto,
        String tipo,
        boolean enviado,
        String error,
        OffsetDateTime createdAt
) {
}
