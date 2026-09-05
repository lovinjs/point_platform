package com.core.coreboot.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.core.coreboot.common.Constant;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;

public class QRCodeGenerator {
    public static String generateQRCode(String text, int width, int height, String fileName) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
        String projectRoot = System.getProperty("user.dir");
        Path generatePath = Paths.get(projectRoot, Constant.QRCODE_GENERATE_PATH).toAbsolutePath().normalize();
        String filePath = generatePath.toString() + "/" + fileName + ".png";
        Path path = FileSystems.getDefault().getPath(filePath);
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
        return fileName + ".png";
    }
}


