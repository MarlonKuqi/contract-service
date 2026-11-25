package com.mk.contractservice.domain.client;

public sealed interface ClientDetails permits PersonDetails, CompanyDetails {

    long version();
}
