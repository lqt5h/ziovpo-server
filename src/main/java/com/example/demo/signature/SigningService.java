package com.example.demo.signature;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Signature;
import java.util.Base64;

/**
 * Фасад модуля ЭЦП.
 * Отвечает за: сбор pipeline, криптоподпись, кодирование результата.
 */
@Service
public class SigningService {

    private final SignatureKeyProvider keyProvider;
    private final JsonCanonicalizationService canonicalizationService;

    @Value("${signature.algorithm:SHA256withRSA}")
    private String algorithm;

    public SigningService(SignatureKeyProvider keyProvider,
                          JsonCanonicalizationService canonicalizationService) {
        this.keyProvider = keyProvider;
        this.canonicalizationService = canonicalizationService;
    }

    /**
     * Подписывает payload:
     * 1. Канонизация → UTF-8 байты (RFC 8785)
     * 2. SHA256withRSA подпись приватным ключом
     * 3. Base64-кодирование результата
     */
    public String sign(Object payload) {
        try {
            byte[] canonicalBytes = canonicalizationService.canonicalize(payload);

            Signature signer = Signature.getInstance(algorithm);
            signer.initSign(keyProvider.getPrivateKey());
            signer.update(canonicalBytes);
            byte[] signatureBytes = signer.sign();

            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (Exception e) {
            throw new RuntimeException("Signing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Проверяет подпись payload
     */
    public boolean verify(Object payload, String signatureBase64) {
        try {
            byte[] canonicalBytes = canonicalizationService.canonicalize(payload);
            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);

            Signature verifier = Signature.getInstance(algorithm);
            verifier.initVerify(keyProvider.getPublicKey());
            verifier.update(canonicalBytes);

            return verifier.verify(signatureBytes);
        } catch (Exception e) {
            throw new RuntimeException("Verification failed: " + e.getMessage(), e);
        }
    }

    /**
     * Подписывает сырые байты (для бинарных данных, без канонизации)
     */
    public byte[] signRawBytes(byte[] data) {
        try {
            Signature signer = Signature.getInstance(algorithm);
            signer.initSign(keyProvider.getPrivateKey());
            signer.update(data);
            return signer.sign();
        } catch (Exception e) {
            throw new RuntimeException("Raw bytes signing failed: " + e.getMessage(), e);
        }
    }

    public String getPublicKeyBase64() {
        return Base64.getEncoder().encodeToString(
                keyProvider.getPublicKey().getEncoded()
        );
    }
}
