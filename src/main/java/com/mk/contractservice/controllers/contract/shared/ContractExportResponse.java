package com.mk.contractservice.controllers.contract.shared;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import java.util.List;

@Schema(description = "One page of the export of active contracts")
public record ContractExportResponse(
        @JsonProperty("items")
        @Schema(description = "Active contracts of this page, in export order")
        List<ContractResponse> items,

        @JsonProperty("nextCursor")
        @Schema(description = "Opaque cursor to send back as the cursor parameter to get the next page; null on the last page",
                example = "AZcBbXyz0AB3kq1Fv2LmNA", nullable = true)
        @Nullable String nextCursor
) {
}
