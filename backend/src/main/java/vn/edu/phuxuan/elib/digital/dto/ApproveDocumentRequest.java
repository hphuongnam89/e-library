package vn.edu.phuxuan.elib.digital.dto;

public record ApproveDocumentRequest(
        boolean approved,
        String note
) {}
