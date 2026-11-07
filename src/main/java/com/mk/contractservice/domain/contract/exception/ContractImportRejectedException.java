package com.mk.contractservice.domain.contract.exception;

import com.mk.contractservice.domain.contract.ContractImportRejectionReason;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
public class ContractImportRejectedException extends RuntimeException {

    public record LineRejection(int index, UUID clientId, ContractImportRejectionReason reason, String message) {
    }

    private final transient List<LineRejection> rejections;

    public ContractImportRejectedException(final List<LineRejection> rejections) {
        super("Contract import rejected: " + rejections.size() + " problem(s) found");
        this.rejections = List.copyOf(rejections);
    }
}
