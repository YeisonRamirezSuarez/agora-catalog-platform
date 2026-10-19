package com.agora.catalog.empresa.service;

import com.agora.catalog.common.random.RandomCodes;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SaveEmpresaServiceTest {

    @Test
    void elSlugSaleDelNombreSinTildesNiSimbolos() {
        assertThat(SaveEmpresaService.slugify("Salsamentaria Javier & Cía.")).isEqualTo("salsamentaria-javier-cia");
        assertThat(SaveEmpresaService.slugify("  Frutería  Doña Rosa ")).isEqualTo("fruteria-dona-rosa");
        assertThat(SaveEmpresaService.slugify("¡¡!!")).isEmpty();
        assertThat(SaveEmpresaService.slugify("a".repeat(100))).hasSize(80);
    }

    @Test
    void codigosAleatoriosConElFormatoEsperado() {
        assertThat(RandomCodes.publicHash()).matches("[a-z0-9]{8}");
        assertThat(RandomCodes.temporaryPassword()).matches("[a-km-zA-HJ-NP-Z2-9]{12}");
    }
}
