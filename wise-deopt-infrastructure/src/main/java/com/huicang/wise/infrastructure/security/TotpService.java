package com.huicang.wise.infrastructure.security;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@Slf4j
public class TotpService {

    private final GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();

    public TotpKey generateSecretKey() {
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        int[] scratchCodes = key.getScratchCodes().stream().mapToInt(Integer::intValue).toArray();
        return new TotpKey(key.getKey(), key.getVerificationCode(), scratchCodes);
    }

    public String generateQrCodeUrl(String issuer, String username, String secret) {
        return GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(issuer, username, new GoogleAuthenticatorKey.Builder(secret).build());
    }

    public byte[] generateQrCodeImage(String qrCodeUrl, int width, int height) throws IOException {
        try {
            com.google.zxing.client.j2se.MatrixToImageWriter.toBufferedImage(
                new com.google.zxing.qrcode.QRCodeWriter().encode(
                    qrCodeUrl,
                    com.google.zxing.BarcodeFormat.QR_CODE,
                    width,
                    height
                )
            );
            
            BufferedImage image = com.google.zxing.client.j2se.MatrixToImageWriter.toBufferedImage(
                new com.google.zxing.qrcode.QRCodeWriter().encode(
                    qrCodeUrl,
                    com.google.zxing.BarcodeFormat.QR_CODE,
                    width,
                    height
                )
            );
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("生成二维码失败", e);
            throw new IOException("生成二维码失败", e);
        }
    }

    public boolean verifyCode(String secret, int code) {
        return googleAuthenticator.authorize(secret, code);
    }

    public static class TotpKey {
        private final String secret;
        private final int verificationCode;
        private final int[] scratchCodes;

        public TotpKey(String secret, int verificationCode, int[] scratchCodes) {
            this.secret = secret;
            this.verificationCode = verificationCode;
            this.scratchCodes = scratchCodes;
        }

        public String getSecret() {
            return secret;
        }

        public int getVerificationCode() {
            return verificationCode;
        }

        public int[] getScratchCodes() {
            return scratchCodes;
        }
    }
}
