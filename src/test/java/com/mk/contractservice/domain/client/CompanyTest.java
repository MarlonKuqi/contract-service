package com.mk.contractservice.domain.client;

import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.CompanyIdentifier;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
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
                Email.of("contact@acme.com"),
                PhoneNumber.of("+33123456789"),
                CompanyIdentifier.of("acme-123")
        );

        assertThat(company).isNotNull();
        assertThat(company.getName().value()).isEqualTo("ACME Corporation");
        assertThat(company.getEmail().value()).isEqualTo("contact@acme.com");
        assertThat(company.getPhone().value()).isEqualTo("+33123456789");
        assertThat(company.getCompanyIdentifier().value()).isEqualTo("acme-123");
    }

    @Test
    @DisplayName("GIVEN null company identifier WHEN creating Company THEN throw exception")
    void shouldRejectNullCompanyIdentifier() {
        assertThatThrownBy(() -> Company.of(
                ClientName.of("ACME Corp"),
                Email.of("contact@acme.com"),
                PhoneNumber.of("+33123456789"),
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Company identifier must not be null");
    }

    @Test
    @DisplayName("GIVEN identifier from subject WHEN creating Company THEN accept")
    void shouldAcceptPatternFromSubject() {
        Company company = Company.of(
                ClientName.of("Example Company"),
                Email.of("info@example.com"),
                PhoneNumber.of("+33987654321"),
                CompanyIdentifier.of("aaa-123")
        );

        assertThat(company.getCompanyIdentifier().value()).isEqualTo("aaa-123");
    }

    @Test
    @DisplayName("GIVEN company identifier with special chars WHEN creating Company THEN accept")
    void shouldAcceptSpecialCharactersInIdentifier() {
        Company company = Company.of(
                ClientName.of("Test Corp"),
                Email.of("test@corp.com"),
                PhoneNumber.of("+33111111111"),
                CompanyIdentifier.of("abc-xyz-123_456")
        );

        assertThat(company.getCompanyIdentifier().value()).isEqualTo("abc-xyz-123_456");
    }

    @Test
    @DisplayName("Should reconstitute Company with ID from database")
    void shouldReconstituteCompanyWithId() {
        UUID id = UUID.randomUUID();
        ClientName name = ClientName.of("ACME Corp");
        Email email = Email.of("contact@acme.com");
        PhoneNumber phone = PhoneNumber.of("+33123456789");
        CompanyIdentifier companyId = CompanyIdentifier.of("acme-123");

        Company company = Company.reconstitute(id, name, email, phone, companyId);

        assertThat(company.getId()).isEqualTo(id);
        assertThat(company.getName()).isEqualTo(name);
        assertThat(company.getEmail()).isEqualTo(email);
        assertThat(company.getPhone()).isEqualTo(phone);
        assertThat(company.getCompanyIdentifier()).isEqualTo(companyId);
    }

    @Test
    @DisplayName("Should reject null ID when reconstituting Company")
    void shouldRejectNullIdOnReconstitute() {
        assertThatThrownBy(() -> Company.reconstitute(
                null,
                ClientName.of("ACME Corp"),
                Email.of("contact@acme.com"),
                PhoneNumber.of("+33123456789"),
                CompanyIdentifier.of("acme-123")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ID must not be null when reconstituting");
    }

    @Test
    @DisplayName("Should update all common fields with withCommonFields")
    void shouldUpdateAllCommonFields() {
        Company company = Company.of(
                ClientName.of("ACME Corp"),
                Email.of("old@acme.com"),
                PhoneNumber.of("+33111111111"),
                CompanyIdentifier.of("acme-123")
        );

        Company updated = company.withCommonFields(
                ClientName.of("ACME Corporation"),
                Email.of("new@acme.com"),
                PhoneNumber.of("+33999999999")
        );

        assertThat(updated.getName().value()).isEqualTo("ACME Corporation");
        assertThat(updated.getEmail().value()).isEqualTo("new@acme.com");
        assertThat(updated.getPhone().value()).isEqualTo("+33999999999");
        assertThat(updated.getCompanyIdentifier().value()).isEqualTo("acme-123");
    }

    @Test
    @DisplayName("Should keep company identifier immutable on withCommonFields")
    void shouldKeepCompanyIdentifierImmutable() {
        CompanyIdentifier originalId = CompanyIdentifier.of("acme-123");
        Company company = Company.of(
                ClientName.of("ACME Corp"),
                Email.of("contact@acme.com"),
                PhoneNumber.of("+33123456789"),
                originalId
        );

        Company updated = company.withCommonFields(
                ClientName.of("New Name"),
                Email.of("new@acme.com"),
                PhoneNumber.of("+33999999999")
        );

        assertThat(updated.getCompanyIdentifier()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should update partial fields keeping others unchanged")
    void shouldUpdatePartialFields() {
        Company company = Company.of(
                ClientName.of("ACME Corp"),
                Email.of("contact@acme.com"),
                PhoneNumber.of("+33123456789"),
                CompanyIdentifier.of("acme-123")
        );

        Company updated = company.updatePartial(
                ClientName.of("ACME Corporation"),
                null,
                null
        );

        assertThat(updated.getName().value()).isEqualTo("ACME Corporation");
        assertThat(updated.getEmail().value()).isEqualTo("contact@acme.com");
        assertThat(updated.getPhone().value()).isEqualTo("+33123456789");
        assertThat(updated.getCompanyIdentifier().value()).isEqualTo("acme-123");
    }

    @Test
    @DisplayName("Should update all fields when all provided in updatePartial")
    void shouldUpdateAllFieldsWhenAllProvided() {
        Company company = Company.of(
                ClientName.of("ACME Corp"),
                Email.of("old@acme.com"),
                PhoneNumber.of("+33111111111"),
                CompanyIdentifier.of("acme-123")
        );

        Company updated = company.updatePartial(
                ClientName.of("New Corp"),
                Email.of("new@corp.com"),
                PhoneNumber.of("+33999999999")
        );

        assertThat(updated.getName().value()).isEqualTo("New Corp");
        assertThat(updated.getEmail().value()).isEqualTo("new@corp.com");
        assertThat(updated.getPhone().value()).isEqualTo("+33999999999");
        assertThat(updated.getCompanyIdentifier().value()).isEqualTo("acme-123");
    }

    @Test
    @DisplayName("Should keep company identifier immutable on updatePartial")
    void shouldKeepCompanyIdentifierImmutableOnUpdatePartial() {
        CompanyIdentifier originalId = CompanyIdentifier.of("acme-123");
        Company company = Company.of(
                ClientName.of("ACME Corp"),
                Email.of("contact@acme.com"),
                PhoneNumber.of("+33123456789"),
                originalId
        );

        Company updated = company.updatePartial(
                ClientName.of("New Name"),
                Email.of("new@acme.com"),
                PhoneNumber.of("+33999999999")
        );

        assertThat(updated.getCompanyIdentifier()).isEqualTo(originalId);
    }
}

