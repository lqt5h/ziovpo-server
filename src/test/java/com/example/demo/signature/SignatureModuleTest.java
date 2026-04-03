package com.example.demo.signature;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SignatureModuleTest {

    private static SignatureKeyProvider keyProvider;
    private static JsonCanonicalizationService canonicalizationService;
    private static SigningService signingService;

    @BeforeAll
    static void setUp() {
        SignatureProperties props = new SignatureProperties();
        props.setKeyStorePath("classpath:test-keystore.p12");
        props.setKeyStoreType("PKCS12");
        props.setKeyStorePassword("testpass");
        props.setKeyAlias("testkey");
        props.setKeyPassword("testpass");

        keyProvider = new SignatureKeyProvider(props);
        keyProvider.init();

        canonicalizationService = new JsonCanonicalizationService();
        signingService = new SigningService(keyProvider, canonicalizationService);

        // Set algorithm field via reflection (normally injected by @Value)
        try {
            Field algorithmField = SigningService.class.getDeclaredField("algorithm");
            algorithmField.setAccessible(true);
            algorithmField.set(signingService, "SHA256withRSA");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ===== Key Provider Tests =====

    @Test
    void keyProvider_loadsPrivateKey() {
        PrivateKey pk = keyProvider.getPrivateKey();
        assertNotNull(pk);
        assertEquals("RSA", pk.getAlgorithm());
    }

    @Test
    void keyProvider_loadsPublicKey() {
        PublicKey pub = keyProvider.getPublicKey();
        assertNotNull(pub);
        assertEquals("RSA", pub.getAlgorithm());
    }

    @Test
    void keyProvider_returnsSameKeysOnMultipleCalls() {
        assertSame(keyProvider.getPrivateKey(), keyProvider.getPrivateKey());
        assertSame(keyProvider.getPublicKey(), keyProvider.getPublicKey());
    }

    // ===== Canonicalization Tests =====

    @Test
    void canonicalize_sortsKeysAlphabetically() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("z", 1);
        payload.put("a", 2);

        byte[] bytes = canonicalizationService.canonicalize(payload);
        String result = new String(bytes, StandardCharsets.UTF_8);

        assertEquals("{\"a\":2,\"z\":1}", result);
    }

    @Test
    void canonicalize_noWhitespace() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("key", "value");

        byte[] bytes = canonicalizationService.canonicalize(payload);
        String result = new String(bytes, StandardCharsets.UTF_8);

        assertFalse(result.contains(" "));
        assertFalse(result.contains("\n"));
    }

    @Test
    void canonicalize_nestedObjects_sortedRecursively() {
        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("b", 2);
        inner.put("a", 1);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("nested", inner);

        byte[] bytes = canonicalizationService.canonicalize(payload);
        String result = new String(bytes, StandardCharsets.UTF_8);

        assertEquals("{\"nested\":{\"a\":1,\"b\":2}}", result);
    }

    @Test
    void canonicalize_arrayWithNestedObjects_sortedRecursively() {
        Map<String, Object> obj1 = new LinkedHashMap<>();
        obj1.put("z", 1);
        obj1.put("a", 2);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("items", List.of(obj1));

        byte[] bytes = canonicalizationService.canonicalize(payload);
        String result = new String(bytes, StandardCharsets.UTF_8);

        assertEquals("{\"items\":[{\"a\":2,\"z\":1}]}", result);
    }

    @Test
    void canonicalize_deterministic() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("x", 10);
        payload.put("a", "hello");
        payload.put("m", true);

        byte[] first = canonicalizationService.canonicalize(payload);
        byte[] second = canonicalizationService.canonicalize(payload);

        assertArrayEquals(first, second);
    }

    @Test
    void canonicalize_utf8Encoding() {
        Map<String, Object> payload = Map.of("key", "Привет");

        byte[] bytes = canonicalizationService.canonicalize(payload);
        String result = new String(bytes, StandardCharsets.UTF_8);

        assertTrue(result.contains("Привет"));
    }

    // ===== Signing Service Tests =====

    @Test
    void sign_returnsBase64String() {
        Map<String, Object> payload = Map.of("data", "test");

        String signature = signingService.sign(payload);

        assertNotNull(signature);
        assertFalse(signature.isEmpty());
        // Verify it's valid Base64
        assertDoesNotThrow(() -> Base64.getDecoder().decode(signature));
    }

    @Test
    void sign_samePayload_sameSignature() {
        Map<String, Object> payload = Map.of("key", "value");

        String sig1 = signingService.sign(payload);
        String sig2 = signingService.sign(payload);

        // RSA подпись детерминирована для одного ключа и данных
        assertEquals(sig1, sig2);
    }

    @Test
    void sign_differentPayloads_differentSignatures() {
        String sig1 = signingService.sign(Map.of("key", "value1"));
        String sig2 = signingService.sign(Map.of("key", "value2"));

        assertNotEquals(sig1, sig2);
    }

    @Test
    void verify_validSignature_returnsTrue() {
        Map<String, Object> payload = Map.of("data", "test");

        String signature = signingService.sign(payload);
        boolean valid = signingService.verify(payload, signature);

        assertTrue(valid);
    }

    @Test
    void verify_tamperedPayload_returnsFalse() {
        Map<String, Object> original = Map.of("data", "test");
        Map<String, Object> tampered = Map.of("data", "hacked");

        String signature = signingService.sign(original);
        boolean valid = signingService.verify(tampered, signature);

        assertFalse(valid);
    }

    @Test
    void verify_corruptedSignature_returnsFalse() {
        Map<String, Object> payload = Map.of("data", "test");
        String signature = signingService.sign(payload);

        // Corrupt one byte
        byte[] sigBytes = Base64.getDecoder().decode(signature);
        sigBytes[0] = (byte) (sigBytes[0] ^ 0xFF);
        String corrupted = Base64.getEncoder().encodeToString(sigBytes);

        boolean valid = signingService.verify(payload, corrupted);
        assertFalse(valid);
    }

    @Test
    void sign_verify_withKeyOrderIndependence() {
        // Payload с ключами в разном порядке должен давать одинаковую подпись
        Map<String, Object> payload1 = new LinkedHashMap<>();
        payload1.put("b", 2);
        payload1.put("a", 1);

        Map<String, Object> payload2 = new LinkedHashMap<>();
        payload2.put("a", 1);
        payload2.put("b", 2);

        String sig1 = signingService.sign(payload1);
        String sig2 = signingService.sign(payload2);

        assertEquals(sig1, sig2, "Canonicalization should make key order irrelevant");
    }

    @Test
    void signRawBytes_producesValidSignature() throws Exception {
        byte[] data = "test data".getBytes(StandardCharsets.UTF_8);

        byte[] sigBytes = signingService.signRawBytes(data);

        assertNotNull(sigBytes);
        assertTrue(sigBytes.length > 0);

        // Verify manually
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(keyProvider.getPublicKey());
        verifier.update(data);
        assertTrue(verifier.verify(sigBytes));
    }
}
