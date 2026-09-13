package vn.edu.phuxuan.elib.digital.dto;

import java.util.List;

public record UpdateDocumentGrantsRequest(
        List<GrantTargetRequest> grants
) {}
