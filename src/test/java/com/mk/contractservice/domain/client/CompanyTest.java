package com.mk.contractservice.domain.client;


import com.mk.contractservice.domain.client.aggregate.Company;
import com.mk.contractservice.domain.client.exception.InvalidClientException;
import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.ClientName;
import com.mk.contractservice.domain.client.valueobject.ClientPhoneNumber;
import com.mk.contractservice.domain.client.valueobject.CompanyIdentifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Company - Domain Entity Tests")
class CompanyTest {

    @Test
    @DisplayName("GIVEN valid data WHEN creating Company THEN company is created")
    void shouldCreateCompanyWithValidData() {
        Company company = Company.of(
                ClientName.of("ACME Corporation"),
                ClientEmail.of("contact@acme.com"),
                ClientPhoneNumber.of("+33123456789"),
                CompanyIdentifier.of("acme-123")
        );

        assertThat(company).isNotNull();
        assertThat(company.getName().getValue()).isEqualTo("ACME Corporation");
        assertThat(company.getEmail().getValue()).isEqualTo("contact@acme.com");
        assertThat(company.getPhone().getValue()).isEqualTo("+33123456789");
        assertThat(company.getCompanyIdentifier().getValue()).isEqualTo("acme-123");
    }

    @Test
    @DisplayName("GIVEN null company identifier WHEN creating Company THEN throw exception")
    void shouldRejectNullCompanyIdentifier() {
        ClientName name = ClientName.of("ACME Corp");
        ClientEmail clientEmail = ClientEmail.of("contact@acme.com");
        ClientPhoneNumber phone = ClientPhoneNumber.of("+33123456789");

        assertThatThrownBy(() -> Company.of(name, clientEmail, phone, null))
                .isInstanceOf(InvalidClientException.class)
                .hasMessage(InvalidClientException.forNullCompanyIdentifier().getMessage());
    }

    @Test
    @DisplayName("GIVEN identifier from subject WHEN creating Company THEN accept")
    void shouldAcceptPatternFromSubject() {
        Company company = Company.of(
                ClientName.of("Example Company"),
                ClientEmail.of("info@example.com"),
                ClientPhoneNumber.of("+33987654321"),
                CompanyIdentifier.of("aaa-123")
        );

        assertThat(company.getCompanyIdentifier().getValue()).isEqualTo("aaa-123");
    }

    @Test
    @DisplayName("GIVEN company identifier with special chars WHEN creating Company THEN accept")
    void shouldAcceptSpecialCharactersInIdentifier() {
        Company company = Company.of(
                ClientName.of("Test Corp"),
                ClientEmail.of("test@corp.com"),
                ClientPhoneNumber.of("+33111111111"),
                CompanyIdentifier.of("abc-xyz-123_456")
        );

        assertThat(company.getCompanyIdentifier().getValue()).isEqualTo("abc-xyz-123_456");
    }

    @Test
    @DisplayName("Should reconstitute Company with ID from database")
    void shouldReconstituteCompanyWithId() {
        UUID id = UUID.randomUUID();
        ClientName name = ClientName.of("ACME Corp");
        ClientEmail clientEmail = ClientEmail.of("contact@acme.com");
        ClientPhoneNumber phone = ClientPhoneNumber.of("+33123456789");
        CompanyIdentifier companyId = CompanyIdentifier.of("acme-123");

        Company company = Company.reconstituteFromDatabase(id, name, clientEmail, phone, companyId);

        assertThat(company.getId()).isEqualTo(id);
        assertThat(company.getName()).isEqualTo(name);
        assertThat(company.getEmail()).isEqualTo(clientEmail);
        assertThat(company.getPhone()).isEqualTo(phone);
        assertThat(company.getCompanyIdentifier()).isEqualTo(companyId);
    }


    @Test
    @DisplayName("Should update all common fields with withCommonFields")
    void shouldUpdateAllCommonFields() {
        Company company = Company.of(
                ClientName.of("ACME Corp"),
                ClientEmail.of("old@acme.com"),
                ClientPhoneNumber.of("+33111111111"),
                CompanyIdentifier.of("acme-123")
        );

        Company updated = company.withCommonFields(
                ClientName.of("ACME Corporation"),
                ClientEmail.of("new@acme.com"),
                ClientPhoneNumber.of("+33999999999")
        );

        assertThat(updated.getName().getValue()).isEqualTo("ACME Corporation");
        assertThat(updated.getEmail().getValue()).isEqualTo("new@acme.com");
        assertThat(updated.getPhone().getValue()).isEqualTo("+33999999999");
        assertThat(updated.getCompanyIdentifier().getValue()).isEqualTo("acme-123");
    }

    @Test
    @DisplayName("Should keep company identifier immutable on withCommonFields")
    void shouldKeepCompanyIdentifierImmutable() {
        CompanyIdentifier originalId = CompanyIdentifier.of("acme-123");
        Company company = Company.of(
                ClientName.of("ACME Corp"),
                ClientEmail.of("contact@acme.com"),
                ClientPhoneNumber.of("+33123456789"),
                originalId
        );

        Company updated = company.withCommonFields(
                ClientName.of("New Name"),
                ClientEmail.of("new@acme.com"),
                ClientPhoneNumber.of("+33999999999")
        );


        assertThat(updated.getCompanyIdentifier()).isEqualTo(originalId);
    }
}

