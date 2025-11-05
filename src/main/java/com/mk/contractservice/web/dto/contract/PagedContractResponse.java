package com.mk.contractservice.web.dto.contract;

import java.util.List;

public record PagedContractResponse(
        List<ContractResponse> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}

