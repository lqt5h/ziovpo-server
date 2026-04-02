package com.example.demo.binary;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Утилита для побайтовой записи примитивов в BigEndian (network byte order).
 * Все многобайтовые значения записываются старшим байтом вперёд.
 */
public final class BinaryWriteUtils {

    private BinaryWriteUtils() {}

    public static void writeU8(ByteArrayOutputStream out, int value) {
        out.write(value & 0xFF);
    }

    public static void writeU16BE(ByteArrayOutputStream out, int value) {
        out.write((value >>> 8) & 0xFF);
        out.write(value & 0xFF);
    }

    public static void writeU32BE(ByteArrayOutputStream out, long value) {
        out.write((int) ((value >>> 24) & 0xFF));
        out.write((int) ((value >>> 16) & 0xFF));
        out.write((int) ((value >>> 8) & 0xFF));
        out.write((int) (value & 0xFF));
    }

    public static void writeI64BE(ByteArrayOutputStream out, long value) {
        out.write((int) ((value >>> 56) & 0xFF));
        out.write((int) ((value >>> 48) & 0xFF));
        out.write((int) ((value >>> 40) & 0xFF));
        out.write((int) ((value >>> 32) & 0xFF));
        out.write((int) ((value >>> 24) & 0xFF));
        out.write((int) ((value >>> 16) & 0xFF));
        out.write((int) ((value >>> 8) & 0xFF));
        out.write((int) (value & 0xFF));
    }

    public static void writeUUID(ByteArrayOutputStream out, UUID uuid) {
        writeI64BE(out, uuid.getMostSignificantBits());
        writeI64BE(out, uuid.getLeastSignificantBits());
    }

    public static void writeAscii(ByteArrayOutputStream out, String text, int fixedLength) {
        byte[] bytes = text.getBytes(StandardCharsets.US_ASCII);
        for (int i = 0; i < fixedLength; i++) {
            out.write(i < bytes.length ? bytes[i] : 0);
        }
    }

    public static void writeLengthPrefixedString(ByteArrayOutputStream out, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeU32BE(out, bytes.length);
        out.writeBytes(bytes);
    }

    public static void writeLengthPrefixedBytes(ByteArrayOutputStream out, byte[] bytes) {
        writeU32BE(out, bytes.length);
        out.writeBytes(bytes);
    }
}
