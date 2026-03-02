package com.example.demo.signature;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "signature")
public class SignatureProperties {

    private String keyStorePath;
    private String keyStoreType = "PKCS12";
    private String keyStorePassword;
    private String keyAlias;
    private String keyPassword;

    public String getKeyStorePath() { return keyStorePath; }
    public String getKeyStoreType() { return keyStoreType; }
    public String getKeyStorePassword() { return keyStorePassword; }
    public String getKeyAlias() { return keyAlias; }
    public String getKeyPassword() { return keyPassword != null ? keyPassword : keyStorePassword; }

    public void setKeyStorePath(String keyStorePath) { this.keyStorePath = keyStorePath; }
    public void setKeyStoreType(String keyStoreType) { this.keyStoreType = keyStoreType; }
    public void setKeyStorePassword(String keyStorePassword) { this.keyStorePassword = keyStorePassword; }
    public void setKeyAlias(String keyAlias) { this.keyAlias = keyAlias; }
    public void setKeyPassword(String keyPassword) { this.keyPassword = keyPassword; }
}
