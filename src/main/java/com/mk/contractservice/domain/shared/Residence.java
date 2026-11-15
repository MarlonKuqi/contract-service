package com.mk.contractservice.domain.shared;

public enum Residence {
    CH, FR, DE, IT, AT;

    public boolean isNotCrossBorder() {
        return this == CH;
    }
}
