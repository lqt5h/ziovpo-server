package com.example.demo.signature;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Канонизация JSON по RFC 8785 (JCS):
 * - без форматирующих пробелов
 * - ключи объектов отсортированы по UTF-16 code units
 * - результат в UTF-8 байтах
 */
@Component
public class JsonCanonicalizationService {

    private final ObjectMapper objectMapper;

    public JsonCanonicalizationService() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Принимает любой объект, возвращает UTF-8 байты канонического JSON
     */
    public byte[] canonicalize(Object payload) {
        try {
            JsonNode node = objectMapper.valueToTree(payload);
            JsonNode sorted = sortObjectKeys(node);
            String canonical = objectMapper
                    .writer()
                    .without(SerializationFeature.INDENT_OUTPUT)
                    .writeValueAsString(sorted);
            return canonical.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalArgumentException("Canonicalization failed: " + e.getMessage(), e);
        }
    }

    /**
     * Рекурсивно сортирует ключи объектов по UTF-16 code units (RFC 8785)
     */
    private JsonNode sortObjectKeys(JsonNode node) {
        if (node.isObject()) {
            // TreeMap сортирует по натуральному порядку строк (Unicode code point order)
            TreeMap<String, JsonNode> sorted = new TreeMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                sorted.put(entry.getKey(), sortObjectKeys(entry.getValue()));
            }
            return objectMapper.valueToTree(sorted);
        }
        if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                sortObjectKeys(node.get(i));
            }
        }
        return node;
    }
}
