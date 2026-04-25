package com.example.demo.service;

import com.example.demo.binary.BinaryWriteUtils;
import com.example.demo.entity.MalwareSignature;
import com.example.demo.entity.SignatureStatus;
import com.example.demo.signature.SigningService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Service
public class BinaryExportService {

    private static final String MANIFEST_MAGIC = "MF-Mareychenko";
    private static final String DATA_MAGIC = "DB-Mareychenko";
    private static final int MAGIC_LENGTH = 14;
    private static final int FORMAT_VERSION = 1;

    private static final int EXPORT_FULL = 0;
    private static final int EXPORT_INCREMENT = 1;
    private static final int EXPORT_BY_IDS = 2;

    private final MalwareSignatureService signatureService;
    private final SigningService signingService;

    public BinaryExportService(MalwareSignatureService signatureService,
                               SigningService signingService) {
        this.signatureService = signatureService;
        this.signingService = signingService;
    }

    // DTO для результата экспорта
    public record BinaryExportResult(byte[] manifest, byte[] data) {}

    // ===== Публичные методы =====

    public BinaryExportResult exportFull() {
        List<MalwareSignature> signatures = signatureService.getAllActual();
        return buildExport(signatures, EXPORT_FULL, -1L);
    }

    public BinaryExportResult exportIncrement(Instant since) {
        List<MalwareSignature> signatures = signatureService.getDiff(since);
        return buildExport(signatures, EXPORT_INCREMENT, since.toEpochMilli());
    }

    public BinaryExportResult exportByIds(List<UUID> ids) {
        List<MalwareSignature> signatures = signatureService.getByIds(ids);
        return buildExport(signatures, EXPORT_BY_IDS, -1L);
    }

    // ===== Построение экспорта =====

    private BinaryExportResult buildExport(List<MalwareSignature> signatures,
                                           int exportType, long sinceEpochMillis) {
        try {
            // 1. Сериализуем каждую запись данных
            List<byte[]> recordBytes = new ArrayList<>();
            for (MalwareSignature sig : signatures) {
                recordBytes.add(serializeDataRecord(sig));
            }

            // 2. Собираем data.bin
            byte[] dataBytes = buildDataBin(recordBytes);

            // 3. SHA-256 от data.bin
            byte[] dataSha256 = MessageDigest.getInstance("SHA-256").digest(dataBytes);

            // 4. Вычисляем смещения записей
            long[] offsets = new long[recordBytes.size()];
            long currentOffset = 0;
            for (int i = 0; i < recordBytes.size(); i++) {
                offsets[i] = currentOffset;
                currentOffset += recordBytes.get(i).length;
            }

            // 5. Собираем неподписанный манифест
            ByteArrayOutputStream unsignedOut = new ByteArrayOutputStream();
            writeManifestHeader(unsignedOut, exportType, sinceEpochMillis,
                    signatures.size(), dataSha256);
            for (int i = 0; i < signatures.size(); i++) {
                writeManifestEntry(unsignedOut, signatures.get(i),
                        offsets[i], recordBytes.get(i).length);
            }
            byte[] unsignedManifest = unsignedOut.toByteArray();

            // 6. Подписываем манифест
            byte[] manifestSignature = signingService.signRawBytes(unsignedManifest);

            // 7. Полный манифест = неподписанный + длина подписи + подпись
            ByteArrayOutputStream fullManifest = new ByteArrayOutputStream();
            fullManifest.write(unsignedManifest);
            BinaryWriteUtils.writeU32BE(fullManifest, manifestSignature.length);
            fullManifest.writeBytes(manifestSignature);

            return new BinaryExportResult(fullManifest.toByteArray(), dataBytes);
        } catch (Exception e) {
            throw new RuntimeException("Binary export failed: " + e.getMessage(), e);
        }
    }

    // ===== data.bin =====

    private byte[] buildDataBin(List<byte[]> recordBytes) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // Заголовок: magic(4) + version(2) + recordCount(4)
        BinaryWriteUtils.writeAscii(out, DATA_MAGIC, MAGIC_LENGTH);
        BinaryWriteUtils.writeU16BE(out, FORMAT_VERSION);
        BinaryWriteUtils.writeU32BE(out, recordBytes.size());
        // Записи
        for (byte[] record : recordBytes) {
            out.writeBytes(record);
        }
        return out.toByteArray();
    }

    private byte[] serializeDataRecord(MalwareSignature sig) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinaryWriteUtils.writeLengthPrefixedString(out, sig.getThreatName());
        BinaryWriteUtils.writeLengthPrefixedBytes(out, hexToBytes(sig.getFirstBytesHex()));
        BinaryWriteUtils.writeLengthPrefixedBytes(out, hexToBytes(sig.getRemainderHashHex()));
        BinaryWriteUtils.writeI64BE(out, sig.getRemainderLength());
        BinaryWriteUtils.writeLengthPrefixedString(out, sig.getFileType());
        BinaryWriteUtils.writeI64BE(out, sig.getOffsetStart());
        BinaryWriteUtils.writeI64BE(out, sig.getOffsetEnd());
        return out.toByteArray();
    }

    // ===== manifest.bin =====

    private void writeManifestHeader(ByteArrayOutputStream out, int exportType,
                                     long sinceEpochMillis, int recordCount,
                                     byte[] dataSha256) {
        // magic(4) + version(2) + exportType(1) + generatedAt(8) + since(8) + count(4) + sha256(32) = 59 bytes
        BinaryWriteUtils.writeAscii(out, MANIFEST_MAGIC, MAGIC_LENGTH);
        BinaryWriteUtils.writeU16BE(out, FORMAT_VERSION);
        BinaryWriteUtils.writeU8(out, exportType);
        BinaryWriteUtils.writeI64BE(out, System.currentTimeMillis());
        BinaryWriteUtils.writeI64BE(out, sinceEpochMillis);
        BinaryWriteUtils.writeU32BE(out, recordCount);
        out.writeBytes(dataSha256);
    }

    private void writeManifestEntry(ByteArrayOutputStream out, MalwareSignature sig,
                                    long dataOffset, int dataLength) {
        // id(16) + status(1) + updatedAt(8) + offset(8) + length(4) + sigLen(4) + sigBytes(var)
        BinaryWriteUtils.writeUUID(out, sig.getId());
        BinaryWriteUtils.writeU8(out, sig.getStatus() == SignatureStatus.ACTUAL ? 0 : 1);
        BinaryWriteUtils.writeI64BE(out, sig.getUpdatedAt().toEpochMilli());
        BinaryWriteUtils.writeI64BE(out, dataOffset);
        BinaryWriteUtils.writeU32BE(out, dataLength);

        String sigBase64 = sig.getDigitalSignatureBase64();
        byte[] recordSigBytes = (sigBase64 != null)
                ? Base64.getDecoder().decode(sigBase64)
                : new byte[0];
        BinaryWriteUtils.writeU32BE(out, recordSigBytes.length);
        out.writeBytes(recordSigBytes);
    }

    // ===== Утилиты =====

    private static byte[] hexToBytes(String hex) {
        return HexFormat.of().parseHex(hex);
    }
}
