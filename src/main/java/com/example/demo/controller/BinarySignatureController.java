package com.example.demo.controller;

import com.example.demo.dto.SignatureIdsRequest;
import com.example.demo.service.BinaryExportService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
    public ResponseEntity<MultiValueMap<String, Object>> getFull() {
        BinaryExportService.BinaryExportResult result = binaryExportService.exportFull();
        return buildMultipartResponse(result);
    }

    @GetMapping("/increment")
    public ResponseEntity<MultiValueMap<String, Object>> getIncrement(@RequestParam("since") String since) {
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
    public ResponseEntity<MultiValueMap<String, Object>> getByIds(@RequestBody SignatureIdsRequest request) {
        List<java.util.UUID> ids = (request.getIds() != null) ? request.getIds() : List.of();
        BinaryExportService.BinaryExportResult result = binaryExportService.exportByIds(ids);
        return buildMultipartResponse(result);
    }

    private ResponseEntity<MultiValueMap<String, Object>> buildMultipartResponse(
            BinaryExportService.BinaryExportResult result) {
        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("manifest", createPart("manifest.bin", result.manifest()));
        body.add("data", createPart("data.bin", result.data()));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("multipart/mixed"));
        return ResponseEntity.ok()
                .headers(headers)
                .body(body);
    }

    private HttpEntity<ByteArrayResource> createPart(String filename, byte[] content) {
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        partHeaders.setContentDisposition(ContentDisposition.attachment().filename(filename).build());

        ByteArrayResource resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        return new HttpEntity<>(resource, partHeaders);
    }
}
