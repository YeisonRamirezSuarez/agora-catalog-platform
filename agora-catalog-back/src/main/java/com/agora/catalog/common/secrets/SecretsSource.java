package com.agora.catalog.common.secrets;

import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Map;

/**
 * De dónde salen los secretos de la aplicación.
 *
 * <p>Existe para que cambiar el origen sea cambiar una propiedad y no tocar código: los
 * servicios siguen leyendo {@code ${DB_PASSWORD}} y compañía sin enterarse de dónde vino.
 * Se resuelve en el arranque, antes de que Spring instancie nada, por eso NO es un
 * {@code @Service}: lo consume {@link SecretsEnvironmentPostProcessor}.
 */
public interface SecretsSource {

    /** Nombre del origen, tal como se escribe en {@code settings.secrets.provider}. */
    String name();

    /**
     * Los secretos, ya resueltos. Un mapa vacío significa "no había nada que aportar"; lanzar
     * significa "esto tenía que funcionar y no funcionó", y ahí la aplicación no arranca.
     */
    Map<String, Object> load(ConfigurableEnvironment environment);

}
