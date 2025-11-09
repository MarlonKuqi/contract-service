package com.mk.contractservice.domain.client;

import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.CompanyIdentifier;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Company - Domain Entity Tests")
class CompanyTest {

    @Test
    @DisplayName("GIVEN valid data WHEN creating Company THEN company is created")
    void shouldCreateCompanyWithValidData() {
        Company company = Company.builder()
                .name(ClientName.of("ACME Corporation"))
                .email(Email.of("contact@acme.com"))
                .phone(PhoneNumber.of("+33123456789"))
                .companyIdentifier(CompanyIdentifier.of("acme-123"))
                .build();

        assertThat(company).isNotNull();
        assertThat(company.getName().value()).isEqualTo("ACME Corporation");
        assertThat(company.getEmail().value()).isEqualTo("contact@acme.com");
        assertThat(company.getPhone().value()).isEqualTo("+33123456789");
        assertThat(company.getCompanyIdentifier().value()).isEqualTo("acme-123");
    }

    @Test
    @DisplayName("GIVEN null company identifier WHEN creating Company THEN throw exception")
    void shouldRejectNullCompanyIdentifier() {
        assertThatThrownBy(() -> Company.builder()
                .name(ClientName.of("ACME Corp"))
                .email(Email.of("contact@acme.com"))
                .phone(PhoneNumber.of("+33123456789"))
                .companyIdentifier(null)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Company identifier must not be null");
    }

    @Test
    @DisplayName("GIVEN identifier from subject WHEN creating Company THEN accept")
    void shouldAcceptPatternFromSubject() {
        Company company = Company.builder()
                .name(ClientName.of("Example Company"))
                .email(Email.of("info@example.com"))
                .phone(PhoneNumber.of("+33987654321"))
                .companyIdentifier(CompanyIdentifier.of("aaa-123"))
                .build();

        assertThat(company.getCompanyIdentifier().value()).isEqualTo("aaa-123");
    }

    @Test
    @DisplayName("GIVEN company identifier with special chars WHEN creating Company THEN accept")
    void shouldAcceptSpecialCharactersInIdentifier() {
        Company company = Company.builder()
                .name(ClientName.of("Test Corp"))
                .email(Email.of("test@corp.com"))
                .phone(PhoneNumber.of("+33111111111"))
                .companyIdentifier(CompanyIdentifier.of("abc-xyz-123_456"))
                .build();

        assertThat(company.getCompanyIdentifier().value()).isEqualTo("abc-xyz-123_456");
    }
}

