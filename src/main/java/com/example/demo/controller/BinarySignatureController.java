package com.example.demo.controller;

import com.example.demo.dto.SignatureIdsRequest;
import com.example.demo.service.BinaryExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/binary/signatures")
public class BinarySignatureController {

    private final BinaryExportService binaryExportService;

    public BinarySignatureController(BinaryExportService binaryExportService) {
        this.binaryExportService = binaryExportService;
    }

    @GetMapping("/full")
    public ResponseEntity<byte[]> getFull() {
        BinaryExportService.BinaryExportResult result = binaryExportService.exportFull();
        return buildMultipartResponse(result);
    }

    @GetMapping("/increment")
    public ResponseEntity<byte[]> getIncrement(@RequestParam("since") String since) {
        Instant sinceInstant;
        try {
            sinceInstant = Instant.parse(since);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Неверный формат параметра since. Используйте ISO-8601, например: 2025-01-01T00:00:00Z");
        }
        BinaryExportService.BinaryExportResult result = binaryExportService.exportIncrement(sinceInstant);
        return buildMultipartResponse(result);
    }

    @PostMapping("/by-ids")
    public ResponseEntity<byte[]> getByIds(@RequestBody SignatureIdsRequest request) {
        List<java.util.UUID> ids = (request.getIds() != null) ? request.getIds() : List.of();
        BinaryExportService.BinaryExportResult result = binaryExportService.exportByIds(ids);
        return buildMultipartResponse(result);
    }

    private ResponseEntity<byte[]> buildMultipartResponse(BinaryExportService.BinaryExportResult result) {
        String boundary = "BinaryExport" + System.currentTimeMillis();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        writePart(out, boundary, "manifest.bin", result.manifest());
        writePart(out, boundary, "data.bin", result.data());

        String closing = "--" + boundary + "--\r\n";
        out.writeBytes(closing.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, "multipart/mixed; boundary=" + boundary);
        return new ResponseEntity<>(out.toByteArray(), headers, HttpStatus.OK);
    }

    private void writePart(ByteArrayOutputStream out, String boundary,
                           String filename, byte[] content) {
        String partHeader = "--" + boundary + "\r\n"
                + "Content-Disposition: attachment; filename=\"" + filename + "\"\r\n"
                + "Content-Type: application/octet-stream\r\n"
                + "Content-Length: " + content.length + "\r\n"
                + "\r\n";
        out.writeBytes(partHeader.getBytes(StandardCharsets.UTF_8));
        out.writeBytes(content);
        out.writeBytes("\r\n".getBytes(StandardCharsets.UTF_8));
    }
}
