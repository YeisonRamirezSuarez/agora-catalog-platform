package com.agora.catalog.common.time;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeZoneEnvironmentPostProcessorTest {

    private final TimeZone original = TimeZone.getDefault();
    private final TimeZoneEnvironmentPostProcessor processor = new TimeZoneEnvironmentPostProcessor();

    @AfterEach
    void restore() {
        TimeZone.setDefault(original);
    }

    @Test
    void sinPropiedadUsaBogota() {
        processor.postProcessEnvironment(new MockEnvironment(), null);

        assertThat(TimeZone.getDefault().getID()).isEqualTo("America/Bogota");
    }

    @Test
    void usaLaZonaConfigurada() {
        processor.postProcessEnvironment(new MockEnvironment().withProperty("settings.time-zone", "America/Lima"), null);

        assertThat(TimeZone.getDefault().getID()).isEqualTo("America/Lima");
    }

    @Test
    void unaZonaInexistenteDetieneElArranqueEnVezDeCaerAGmt() {
        MockEnvironment environment = new MockEnvironment().withProperty("settings.time-zone", "America/Bogotá");

        assertThatThrownBy(() -> processor.postProcessEnvironment(environment, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("America/Bogotá");
        assertThat(TimeZone.getDefault()).isEqualTo(original);
    }

}
