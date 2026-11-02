package com.mk.contractservice.features.contract;

import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
public class ContractImportRejectedException extends RuntimeException {

    public enum Reason {
        INVALID_COST,
        INVALID_PERIOD,
        CLIENT_NOT_FOUND
    }

    public record LineRejection(int index, UUID clientId, Reason reason, String message) {
    }

    private final transient List<LineRejection> rejections;

    public ContractImportRejectedException(final List<LineRejection> rejections) {
        super("Contract import rejected: " + rejections.size() + " problem(s) found");
        this.rejections = List.copyOf(rejections);
    }
}
