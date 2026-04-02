package com.example.demo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class SignatureRequest {

    @NotBlank(message = "threatName не должен быть пустым")
    private String threatName;

    @NotBlank(message = "firstBytesHex не должен быть пустым")
    @Pattern(regexp = "^[0-9a-fA-F]+$", message = "firstBytesHex должен содержать только hex-символы")
    private String firstBytesHex;

    @NotBlank(message = "remainderHashHex не должен быть пустым")
    @Pattern(regexp = "^[0-9a-fA-F]+$", message = "remainderHashHex должен содержать только hex-символы")
    private String remainderHashHex;

    @Min(value = 0, message = "remainderLength должен быть >= 0")
    private long remainderLength;

    @NotBlank(message = "fileType не должен быть пустым")
    private String fileType;

    @Min(value = 0, message = "offsetStart должен быть >= 0")
    private long offsetStart;

    @Min(value = 0, message = "offsetEnd должен быть >= 0")
    private long offsetEnd;

    public SignatureRequest() {}

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
}
