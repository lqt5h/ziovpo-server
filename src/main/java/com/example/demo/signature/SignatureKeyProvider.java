package com.example.demo.signature;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class SignatureKeyProvider {

    private final SignatureProperties properties;
    private final ReentrantLock lock = new ReentrantLock();

    private volatile PrivateKey privateKey;
    private volatile PublicKey publicKey;

    public SignatureKeyProvider(SignatureProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        loadKeys();
    }

    private void loadKeys() {
        lock.lock();
        try {
            if (privateKey != null && publicKey != null) return;

            KeyStore keyStore = KeyStore.getInstance(properties.getKeyStoreType());
            Resource resource = new DefaultResourceLoader().getResource(properties.getKeyStorePath());

            keyStore.load(
                    resource.getInputStream(),
                    properties.getKeyStorePassword().toCharArray()
            );

            privateKey = (PrivateKey) keyStore.getKey(
                    properties.getKeyAlias(),
                    properties.getKeyPassword().toCharArray()
            );

            if (privateKey == null) {
                throw new IllegalStateException(
                        "Private key not found for alias: " + properties.getKeyAlias());
            }

            Certificate cert = keyStore.getCertificate(properties.getKeyAlias());
            if (cert == null) {
                throw new IllegalStateException(
                        "Certificate not found for alias: " + properties.getKeyAlias());
            }

            publicKey = cert.getPublicKey();

        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load keystore: " + e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    public PrivateKey getPrivateKey() {
        if (privateKey == null) loadKeys();
        return privateKey;
    }

    public PublicKey getPublicKey() {
        if (publicKey == null) loadKeys();
        return publicKey;
    }
}
