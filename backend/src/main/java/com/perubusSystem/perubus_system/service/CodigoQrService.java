package com.perubusSystem.perubus_system.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Locale;

@Service
public class CodigoQrService {

    private static final int TAMANO_QR = 320;

    public byte[] generarCodigoQr(String codigoTracking) throws WriterException, IOException {
        String tracking = normalizarTracking(codigoTracking);
        Map<EncodeHintType, Object> opciones = Map.of(
                EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN, 4);
        BitMatrix matriz = new QRCodeWriter().encode(
                tracking,
                BarcodeFormat.QR_CODE,
                TAMANO_QR,
                TAMANO_QR,
                opciones);

        BufferedImage imagen = new BufferedImage(TAMANO_QR, TAMANO_QR, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < TAMANO_QR; x++) {
            for (int y = 0; y < TAMANO_QR; y++) {
                imagen.setRGB(x, y, matriz.get(x, y) ? 0x000000 : 0xFFFFFF);
            }
        }

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        if (!ImageIO.write(imagen, "PNG", salida)) {
            throw new IOException("No hay un codificador PNG disponible.");
        }
        return salida.toByteArray();
    }

    public static String normalizarTracking(String codigoTracking) {
        String tracking = codigoTracking == null ? "" : codigoTracking.trim().toUpperCase(Locale.ROOT);
        if (!tracking.matches("PERU-[A-Z0-9]{8}")) {
            throw new IllegalArgumentException("El código de tracking no tiene un formato válido.");
        }
        return tracking;
    }
}
