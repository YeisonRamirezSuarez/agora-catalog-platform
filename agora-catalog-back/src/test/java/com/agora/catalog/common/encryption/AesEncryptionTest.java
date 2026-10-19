package com.agora.catalog.common.encryption;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesEncryptionTest {

    private final AesEncryption aes = new AesEncryption("clave-de-prueba");

    @Test
    void cifraYDescifraConUnIvDistintoCadaVez() {
        String a = aes.encrypt("contraseña-smtp");
        String b = aes.encrypt("contraseña-smtp");

        assertThat(a).isNotEqualTo(b).doesNotContain("contraseña");
        assertThat(aes.decrypt(a)).isEqualTo("contraseña-smtp");
        assertThat(aes.decrypt(b)).isEqualTo("contraseña-smtp");
    }

    @Test
    void conOtraClaveOAlteradoNoSeLee() {
        String cifrado = aes.encrypt("secreto");

        assertThatThrownBy(() -> new AesEncryption("otra-clave").decrypt(cifrado))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("AES_KEY");
        String alterado = (cifrado.charAt(20) == 'A' ? 'B' : 'A') + "";
        assertThatThrownBy(() -> aes.decrypt(cifrado.substring(0, 20) + alterado + cifrado.substring(21)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void sinClaveNoArranca() {
        assertThatThrownBy(() -> new AesEncryption(" ")).isInstanceOf(IllegalStateException.class);
    }
}
