package com.mk.contractservice.web.dto.client;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = PersonResponse.class, name = "PERSON"),
        @JsonSubTypes.Type(value = CompanyResponse.class, name = "COMPANY")
})
@Schema(
        oneOf = {PersonResponse.class, CompanyResponse.class},
        discriminatorProperty = "type",
        description = "Client can be either a Person or a Company"
)
public sealed interface ClientResponse permits PersonResponse, CompanyResponse {
}
