package com.inix.omqweb.Util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;
import java.util.Random;

@Service
public class AESUtil {
    private final String ALGORITHM = "AES";

    @Value("${aes.secretKey}")
    private String secretKey;

    @Scheduled(fixedRate = 1000 * 60 * 60 * 24)
    public void generateRandomKey() {
        Random random = new Random();
        byte[] randomBytes = new byte[16];
        random.nextBytes(randomBytes);
        secretKey = Base64.getUrlEncoder().encodeToString(randomBytes);
    }

    public String encrypt(String valueToEnc) {
        try {
            Key key = generateKey();
            Cipher c = Cipher.getInstance(ALGORITHM);
            c.init(Cipher.ENCRYPT_MODE, key);
            byte[] encValue = c.doFinal(valueToEnc.getBytes());

            return Base64.getUrlEncoder().encodeToString(encValue);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt the value:" + valueToEnc);
        }
    }

    public String decrypt(String encryptedValue) {
        try {
            Key key = generateKey();
            Cipher c = Cipher.getInstance(ALGORITHM);
            c.init(Cipher.DECRYPT_MODE, key);
            byte[] decodedValue = Base64.getUrlDecoder().decode(encryptedValue);
            byte[] decValue = c.doFinal(decodedValue);
            return new String(decValue);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt the value:" + encryptedValue);
        }
    }

    private Key generateKey() throws Exception {
        Key key = new SecretKeySpec(secretKey.getBytes(), ALGORITHM);
        return key;
    }
}