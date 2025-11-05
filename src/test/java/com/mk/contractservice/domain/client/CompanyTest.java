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
        Company company = new Company(
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
        assertThatThrownBy(() -> new Company(
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
        Company company = new Company(
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
        Company company = new Company(
                ClientName.of("Test Corp"),
                Email.of("test@corp.com"),
                PhoneNumber.of("+33111111111"),
                CompanyIdentifier.of("abc-xyz-123_456")
        );

        assertThat(company.getCompanyIdentifier().value()).isEqualTo("abc-xyz-123_456");
    }
}

