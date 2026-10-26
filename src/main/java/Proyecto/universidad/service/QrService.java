package Proyecto.universidad.service;

import Proyecto.universidad.model.Equipo;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * El QR contiene únicamente el código de inventario del equipo.
 * Sirve solo para IDENTIFICAR: nunca modifica estados (RN-19).
 */
public class QrService {

    private final EquipoService equipoService = new EquipoService();

    public BufferedImage generarImagen(String codigo, int tamano) throws Exception {
        if (codigo == null || codigo.isBlank()) {
            throw new Exception("El equipo no tiene código de inventario.");
        }

        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 2);

        try {
            BitMatrix matriz = new QRCodeWriter()
                    .encode(codigo.trim(), BarcodeFormat.QR_CODE, tamano, tamano, hints);
            return MatrixToImageWriter.toBufferedImage(matriz);
        } catch (WriterException e) {
            throw new Exception("No se pudo generar el código QR.");
        }
    }

    public void guardarPng(BufferedImage imagen, File destino) throws Exception {
        try {
            if (!ImageIO.write(imagen, "png", destino)) {
                throw new Exception("No se pudo guardar la imagen del QR.");
            }
        } catch (IOException e) {
            throw new Exception("No se pudo guardar la imagen del QR en la ubicación elegida.");
        }
    }

    public String leerCodigo(File archivo) throws Exception {
        BufferedImage imagen;
        try {
            imagen = ImageIO.read(archivo);
        } catch (IOException e) {
            throw new Exception("No se pudo abrir la imagen seleccionada.");
        }
        if (imagen == null) {
            throw new Exception("El archivo seleccionado no es una imagen válida (use PNG o JPG).");
        }

        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(imagen)));

        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, List.of(BarcodeFormat.QR_CODE));

        try {
            Result resultado = new MultiFormatReader().decode(bitmap, hints);
            return resultado.getText().trim();
        } catch (NotFoundException e) {
            throw new Exception("No se encontró un código QR legible en la imagen.");
        }
    }

    // Lee el QR y consulta el equipo REAL en la base de datos
    public Equipo identificarEquipo(File archivo) throws Exception {
        String codigo = leerCodigo(archivo);

        Equipo equipo = equipoService.buscarPorCodigo(codigo);
        if (equipo == null) {
            throw new Exception("El QR corresponde al código \"" + codigo + "\", que no existe en el sistema.");
        }
        return equipo;
    }

    // Carpeta donde se guardan y se buscan las imágenes QR (se crea si no existe)
    public File carpetaQr() {
        File carpeta = new File("ImagenesQR").getAbsoluteFile();
        if (!carpeta.exists()) {
            carpeta.mkdirs();
        }
        return carpeta;
    }
}