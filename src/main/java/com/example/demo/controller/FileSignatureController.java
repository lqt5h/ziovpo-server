package com.example.demo.controller;

import com.example.demo.dto.SignatureIdsRequest;
import com.example.demo.entity.MalwareSignature;
import com.example.demo.entity.SignatureStatus;
import com.example.demo.repository.MalwareSignatureRepository;
import com.example.demo.service.MinioService;
import com.example.demo.signature.SigningService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/files/signatures")
@PreAuthorize("hasRole('ADMIN')")
public class FileSignatureController {

    private static final int FIRST_BYTES_LENGTH = 8;

    private final MinioService minioService;
    private final MalwareSignatureRepository signatureRepository;
    private final SigningService signingService;

    public FileSignatureController(MinioService minioService,
                                   MalwareSignatureRepository signatureRepository,
                                   SigningService signingService) {
        this.minioService = minioService;
        this.signatureRepository = signatureRepository;
        this.signingService = signingService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadAndCreateSignature(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "threatName", required = false) String threatName,
            @RequestParam(value = "fileType", required = false) String fileType,
            @RequestParam(value = "offsetStart", defaultValue = "0") long offsetStart,
            @RequestParam(value = "offsetEnd", defaultValue = "-1") long offsetEnd) {

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }

        try {
            byte[] fileBytes = file.getBytes();

            if (threatName == null || threatName.isBlank()) {
                threatName = file.getOriginalFilename() != null
                        ? file.getOriginalFilename() : "unknown";
            }
            if (fileType == null || fileType.isBlank()) {
                fileType = extractExtension(file.getOriginalFilename());
            }
            if (offsetEnd < 0) {
                offsetEnd = fileBytes.length;
            }
            if (offsetEnd < offsetStart) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "offsetEnd must be >= offsetStart");
            }

            int firstLen = Math.min(FIRST_BYTES_LENGTH, fileBytes.length);
            byte[] firstBytes = Arrays.copyOf(fileBytes, firstLen);
            String firstBytesHex = bytesToHex(firstBytes);

            byte[] remainder = firstLen < fileBytes.length
                    ? Arrays.copyOfRange(fileBytes, firstLen, fileBytes.length)
                    : new byte[0];
            String remainderHashHex = bytesToHex(sha256(remainder));

            MalwareSignature signature = new MalwareSignature();
            signature.setThreatName(threatName);
            signature.setFirstBytesHex(firstBytesHex);
            signature.setRemainderHashHex(remainderHashHex);
            signature.setRemainderLength(remainder.length);
            signature.setFileType(fileType);
            signature.setOffsetStart(offsetStart);
            signature.setOffsetEnd(offsetEnd);
            signature.setStatus(SignatureStatus.ACTUAL);

            Map<String, Object> sigPayload = buildSigningPayload(signature);
            String digitalSig = signingService.sign(sigPayload);
            signature.setDigitalSignatureBase64(digitalSig);
            signature.setUpdatedAt(Instant.now());

            MalwareSignature saved = signatureRepository.save(signature);

            String objectName = saved.getId().toString();
            minioService.uploadBytes(objectName, fileBytes,
                    file.getContentType() != null ? file.getContentType() : "application/octet-stream");

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("id", saved.getId());
            response.put("threatName", saved.getThreatName());
            response.put("fileType", saved.getFileType());
            response.put("firstBytesHex", saved.getFirstBytesHex());
            response.put("remainderHashHex", saved.getRemainderHashHex());
            response.put("remainderLength", saved.getRemainderLength());
            response.put("status", saved.getStatus());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to process file: " + e.getMessage());
        }
    }

    @PostMapping("/presigned-urls")
    public ResponseEntity<Map<UUID, String>> getPresignedUrls(
            @RequestBody SignatureIdsRequest request) {

        if (request.getIds() == null || request.getIds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ids list is empty");
        }

        List<MalwareSignature> signatures = signatureRepository.findAllByIdIn(request.getIds());

        Map<UUID, String> urls = new LinkedHashMap<>();
        for (MalwareSignature sig : signatures) {
            String objectName = sig.getId().toString();
            String url = minioService.getPresignedUrl(objectName);
            urls.put(sig.getId(), url);
        }

        return ResponseEntity.ok(urls);
    }

    private Map<String, Object> buildSigningPayload(MalwareSignature sig) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("threatName", sig.getThreatName());
        payload.put("firstBytesHex", sig.getFirstBytesHex());
        payload.put("remainderHashHex", sig.getRemainderHashHex());
        payload.put("remainderLength", sig.getRemainderLength());
        payload.put("fileType", sig.getFileType());
        payload.put("offsetStart", sig.getOffsetStart());
        payload.put("offsetEnd", sig.getOffsetEnd());
        payload.put("status", sig.getStatus().name());
        return payload;
    }

    private static String extractExtension(String filename) {
        if (filename == null) return "bin";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1) : "bin";
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
