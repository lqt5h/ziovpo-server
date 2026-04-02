package com.example.demo.dto;

import com.example.demo.entity.MalwareSignature;
import com.example.demo.entity.SignatureStatus;

import java.time.Instant;
import java.util.UUID;

public class SignatureResponse {

    private UUID id;
    private String threatName;
    private String firstBytesHex;
    private String remainderHashHex;
    private long remainderLength;
    private String fileType;
    private long offsetStart;
    private long offsetEnd;
    private Instant updatedAt;
    private SignatureStatus status;
    private String digitalSignatureBase64;

    public SignatureResponse() {}

    public static SignatureResponse fromEntity(MalwareSignature entity) {
        SignatureResponse dto = new SignatureResponse();
        dto.setId(entity.getId());
        dto.setThreatName(entity.getThreatName());
        dto.setFirstBytesHex(entity.getFirstBytesHex());
        dto.setRemainderHashHex(entity.getRemainderHashHex());
        dto.setRemainderLength(entity.getRemainderLength());
        dto.setFileType(entity.getFileType());
        dto.setOffsetStart(entity.getOffsetStart());
        dto.setOffsetEnd(entity.getOffsetEnd());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setStatus(entity.getStatus());
        dto.setDigitalSignatureBase64(entity.getDigitalSignatureBase64());
        return dto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getThreatName() { return threatName; }
    public void setThreatName(String threatName) { this.threatName = threatName; }

    public String getFirstBytesHex() { return firstBytesHex; }
    public void setFirstBytesHex(String firstBytesHex) { this.firstBytesHex = firstBytesHex; }

    public String getRemainderHashHex() { return remainderHashHex; }
    public void setRemainderHashHex(String remainderHashHex) { this.remainderHashHex = remainderHashHex; }

    public long getRemainderLength() { return remainderLength; }
    public void setRemainderLength(long remainderLength) { this.remainderLength = remainderLength; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public long getOffsetStart() { return offsetStart; }
    public void setOffsetStart(long offsetStart) { this.offsetStart = offsetStart; }

    public long getOffsetEnd() { return offsetEnd; }
    public void setOffsetEnd(long offsetEnd) { this.offsetEnd = offsetEnd; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public SignatureStatus getStatus() { return status; }
    public void setStatus(SignatureStatus status) { this.status = status; }

    public String getDigitalSignatureBase64() { return digitalSignatureBase64; }
    public void setDigitalSignatureBase64(String digitalSignatureBase64) { this.digitalSignatureBase64 = digitalSignatureBase64; }
}
