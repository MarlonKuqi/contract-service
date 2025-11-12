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
        @JsonSubTypes.Type(value = CreatePersonResponse.class, name = "PERSON"),
        @JsonSubTypes.Type(value = CreateCompanyResponse.class, name = "COMPANY")
})
@Schema(
        description = "Create client response (Person or Company)",
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "PERSON", schema = CreatePersonResponse.class),
                @DiscriminatorMapping(value = "COMPANY", schema = CreateCompanyResponse.class)
        },
        oneOf = {CreatePersonResponse.class, CreateCompanyResponse.class}
)
public sealed interface CreateClientResponse permits CreatePersonResponse, CreateCompanyResponse {
}

