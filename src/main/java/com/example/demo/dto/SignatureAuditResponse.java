package com.example.demo.dto;

import com.example.demo.entity.MalwareSignatureAudit;

import java.time.Instant;
import java.util.UUID;

public class SignatureAuditResponse {

    private Long auditId;
    private UUID signatureId;
    private String changedBy;
    private Instant changedAt;
    private String fieldsChanged;
    private String description;

    public SignatureAuditResponse() {}

    public static SignatureAuditResponse fromEntity(MalwareSignatureAudit entity) {
        SignatureAuditResponse dto = new SignatureAuditResponse();
        dto.setAuditId(entity.getAuditId());
        dto.setSignatureId(entity.getSignatureId());
        dto.setChangedBy(entity.getChangedBy());
        dto.setChangedAt(entity.getChangedAt());
        dto.setFieldsChanged(entity.getFieldsChanged());
        dto.setDescription(entity.getDescription());
        return dto;
    }

    public Long getAuditId() { return auditId; }
    public void setAuditId(Long auditId) { this.auditId = auditId; }

    public UUID getSignatureId() { return signatureId; }
    public void setSignatureId(UUID signatureId) { this.signatureId = signatureId; }

    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }

    public Instant getChangedAt() { return changedAt; }
    public void setChangedAt(Instant changedAt) { this.changedAt = changedAt; }

    public String getFieldsChanged() { return fieldsChanged; }
    public void setFieldsChanged(String fieldsChanged) { this.fieldsChanged = fieldsChanged; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
