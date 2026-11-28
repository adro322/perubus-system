package com.perubusSystem.perubus_system.service;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodigoQrServiceTest {

    private final CodigoQrService service = new CodigoQrService();

    @Test
    void generaPngQueCodificaElCodigoDeTracking() throws Exception {
        byte[] png = service.generarCodigoQr(" peru-abcdefgh ");
        var imagen = ImageIO.read(new ByteArrayInputStream(png));
        int[] pixeles = imagen.getRGB(0, 0, imagen.getWidth(), imagen.getHeight(), null, 0, imagen.getWidth());
        var bitmap = new BinaryBitmap(new HybridBinarizer(
                new RGBLuminanceSource(imagen.getWidth(), imagen.getHeight(), pixeles)));

        assertThat(png).startsWith((byte) 0x89, (byte) 0x50, (byte) 0x4e, (byte) 0x47);
        assertThat(imagen.getWidth()).isEqualTo(320);
        assertThat(new MultiFormatReader().decode(bitmap).getText()).isEqualTo("PERU-ABCDEFGH");
    }

    @Test
    void rechazaCodigosDeTrackingConFormatoInvalido() {
        assertThatThrownBy(() -> service.generarCodigoQr("PERU-INVALID"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("formato válido");
    }
}
