package com.mk.contractservice.web.dto.client;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CreatePersonRequest.class, name = "PERSON"),
        @JsonSubTypes.Type(value = CreateCompanyRequest.class, name = "COMPANY")
})
@Schema(
        description = "Create client request (Person or Company)",
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "PERSON", schema = CreatePersonRequest.class),
                @DiscriminatorMapping(value = "COMPANY", schema = CreateCompanyRequest.class)
        },
        oneOf = {CreatePersonRequest.class, CreateCompanyRequest.class}
)
public sealed interface CreateClientRequest permits CreatePersonRequest, CreateCompanyRequest {
    String name();

    String email();

    String phone();
}

