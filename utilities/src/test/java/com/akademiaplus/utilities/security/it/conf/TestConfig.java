package com.akademiaplus.utilities.security.it.conf;

import com.akademiaplus.utilities.security.AESGCMEncryptionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestConfig {

    @Bean
    public AESGCMEncryptionService encryptionService(
            @Value("${security.encryption-key}") String base64Key) {
        // The implementation here shows that it needs the dependency to be created
        return new AESGCMEncryptionService(base64Key);
    }
}