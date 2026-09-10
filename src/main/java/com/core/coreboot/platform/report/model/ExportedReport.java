package com.core.coreboot.platform.report.model;

public record ExportedReport(
        String fileName,
        String contentType,
        byte[] content
) {
    public ExportedReport {
        content = content == null ? new byte[0] : content.clone();
    }

    @Override
    public byte[] content() {
        return content.clone();
    }
}
