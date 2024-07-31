package com.inix.omqweb.Util;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
class AESUtilTest {
    @Autowired
    AESUtil aesUtil;

    @Test
    void encrypt() {
    }

    @Test
    void decrypt() {
        String value = "M2wORCfpasH-G0PtKm-s_w==";
        String decryptedValue = aesUtil.decrypt(value);
        System.out.println(decryptedValue);
    }
}