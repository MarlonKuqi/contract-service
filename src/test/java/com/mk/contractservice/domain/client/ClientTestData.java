package com.mk.contractservice.domain.client;

import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.ClientName;
import com.mk.contractservice.domain.client.valueobject.CompanyIdentifier;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class ClientTestData {

    // ==================== Common Valid Client Data (Generic) ====================

    public record CommonClientData(String name, String email, String phone, String expectedPhone) {
    }

    public static List<CommonClientData> validCommonClients() {
        return List.of(
                new CommonClientData("Jean-François Dupont", "jean.dupont@example.fr", "+33 1 23 45 67 89", "+33123456789"),
                new CommonClientData("Marie Martin", "marie.martin@example.fr", "+33612345678", "+33612345678"),
                new CommonClientData("Hans Müller", "hans.mueller@example.de", "+49 30 12345678", "+493012345678"),
                new CommonClientData("Giuseppe Rossi", "giuseppe.rossi@example.it", "+39 02 1234 5678", "+390212345678"),
                new CommonClientData("Luca Bianchi", "luca.bianchi@example.it", "+39345123456", "+39345123456"),
                new CommonClientData("Klaus Schmidt", "klaus.schmidt@example.de", "+49 89 123456789", "+4989123456789"),
                new CommonClientData("Sophie Lefèvre", "sophie.lefevre@example.fr", "+33 9 87 65 43 21", "+33987654321"),
                new CommonClientData("Anna Weber", "anna.weber@example.ch", "+41 21 123 45 67", "+41211234567"),
                new CommonClientData("Pierre Dubois", "pierre.dubois@example.ch", "+41791234567", "+41791234567")
        );
    }

    public static Stream<Object[]> validCommonClientsAsArguments() {
        return validCommonClients().stream()
                .map(client -> new Object[]{client});
    }

    // ==================== Person-Specific Data ====================

    public record ValidPersonData(String name, String email, String phone, String expectedPhone, LocalDate birthDate) {
        public static ValidPersonData from(CommonClientData common, LocalDate birthDate) {
            return new ValidPersonData(common.name(), common.email(), common.phone(), common.expectedPhone(), birthDate);
        }
    }

    public static List<ValidPersonData> validPersons() {
        var commonClients = validCommonClients();
        return List.of(
                ValidPersonData.from(commonClients.get(0), LocalDate.of(1985, 3, 15)),
                ValidPersonData.from(commonClients.get(1), LocalDate.of(1990, 7, 22)),
                ValidPersonData.from(commonClients.get(2), LocalDate.of(1978, 11, 5)),
                ValidPersonData.from(commonClients.get(3), LocalDate.of(1982, 1, 30)),
                ValidPersonData.from(commonClients.get(4), LocalDate.of(1995, 6, 12)),
                ValidPersonData.from(commonClients.get(5), LocalDate.of(1970, 9, 18)),
                ValidPersonData.from(commonClients.get(6), LocalDate.of(2000, 2, 29)), // Leap year
                ValidPersonData.from(commonClients.get(7), LocalDate.of(1988, 12, 25)),
                ValidPersonData.from(commonClients.get(8), LocalDate.now().minusYears(18)) // Exactly 18
        );
    }

    // ==================== Company-Specific Data ====================

    public record ValidCompanyData(String name, String email, String phone, String expectedPhone,
                                   String companyIdentifier) {
        public static ValidCompanyData from(CommonClientData common, String companyIdentifier) {
            return new ValidCompanyData(common.name(), common.email(), common.phone(), common.expectedPhone(), companyIdentifier);
        }
    }

    public static List<ValidCompanyData> validCompanies() {
        var commonClients = validCommonClients();
        return List.of(
                ValidCompanyData.from(commonClients.get(0), "CHE-123.456.789"),
                ValidCompanyData.from(commonClients.get(1), "DE-987654321"),
                ValidCompanyData.from(commonClients.get(2), "FR-123456789"),
                ValidCompanyData.from(commonClients.get(3), "IT-456789123"),
                ValidCompanyData.from(commonClients.get(4), "CHE-987.654.321"),
                ValidCompanyData.from(commonClients.get(5), "x"),
                ValidCompanyData.from(commonClients.get(6), "a".repeat(CompanyIdentifier.MAX_LENGTH)) // Max length
        );
    }

    // ==================== Invalid Person Data ====================

    public record InvalidPersonData(String name, String email, String phone, LocalDate birthDate) {
    }

    public static List<InvalidPersonData> invalidPersons() {
        return List.of(
                // Invalid names
                new InvalidPersonData(null, "test@example.com", "+41211234567", LocalDate.of(1990, 1, 1)),
                new InvalidPersonData("", "test@example.com", "+41211234567", LocalDate.of(1990, 1, 1)),
                new InvalidPersonData("   ", "test@example.com", "+41211234567", LocalDate.of(1990, 1, 1)),
                new InvalidPersonData("a".repeat(201), "test@example.com", "+41211234567", LocalDate.of(1990, 1, 1)),

                // Invalid emails
                new InvalidPersonData("John Doe", null, "+41211234567", LocalDate.of(1990, 1, 1)),
                new InvalidPersonData("John Doe", "", "+41211234567", LocalDate.of(1990, 1, 1)),
                new InvalidPersonData("John Doe", "invalid-email", "+41211234567", LocalDate.of(1990, 1, 1)),
                new InvalidPersonData("John Doe", "test@", "+41211234567", LocalDate.of(1990, 1, 1)),

                // Invalid phones
                new InvalidPersonData("John Doe", "test@example.com", null, LocalDate.of(1990, 1, 1)),
                new InvalidPersonData("John Doe", "test@example.com", "", LocalDate.of(1990, 1, 1)),
                new InvalidPersonData("John Doe", "test@example.com", "0211234567", LocalDate.of(1990, 1, 1)),
                new InvalidPersonData("John Doe", "test@example.com", "+1234567890", LocalDate.of(1990, 1, 1)),
                new InvalidPersonData("John Doe", "test@example.com", "+44 20 7946 0958", LocalDate.of(1990, 1, 1)),

                // Invalid birth dates
                new InvalidPersonData("John Doe", "test@example.com", "+41211234567", null),
                new InvalidPersonData("John Doe", "test@example.com", "+41211234567", LocalDate.now().plusDays(1)),
                new InvalidPersonData("John Doe", "test@example.com", "+41211234567", LocalDate.of(2100, 1, 1))
        );
    }

    // ==================== Invalid Company Data ====================

    public record InvalidCompanyData(String name, String email, String phone, String companyIdentifier) {
    }

    public static List<InvalidCompanyData> invalidCompanies() {
        return List.of(
                // Invalid names
                new InvalidCompanyData(null, "test@example.com", "+41211234567", "CHE-123.456.789"),
                new InvalidCompanyData("", "test@example.com", "+41211234567", "CHE-123.456.789"),

                // Invalid emails
                new InvalidCompanyData("ACME Corp", null, "+41211234567", "CHE-123.456.789"),
                new InvalidCompanyData("ACME Corp", "invalid", "+41211234567", "CHE-123.456.789"),

                // Invalid phones
                new InvalidCompanyData("ACME Corp", "test@example.com", null, "CHE-123.456.789"),
                new InvalidCompanyData("ACME Corp", "test@example.com", "+86 10 1234 5678", "CHE-123.456.789"),

                // Invalid company identifiers
                new InvalidCompanyData("ACME Corp", "test@example.com", "+41211234567", null),
                new InvalidCompanyData("ACME Corp", "test@example.com", "+41211234567", ""),
                new InvalidCompanyData("ACME Corp", "test@example.com", "+41211234567", "a".repeat(65))
        );
    }

    public static List<String> invalidNames() {
        return Arrays.asList(
                null,
                "",
                "   ",
                "a".repeat(ClientName.MAX_LENGTH + 1) // 201 chars
        );
    }

    public static List<String> invalidEmails() {
        return Arrays.asList(
                null,
                "",
                "invalid-email",
                "johnexample.com",
                "contact@",
                "contactacme.com",
                "a".repeat(ClientEmail.MAX_LENGTH) + "@example.com" // Exceeds RFC 5321 (254 chars)
        );
    }

    public static List<String> invalidPhones() {
        return Arrays.asList(
                null,
                "",
                "123456789",                    // Missing international prefix
                "0211234567",                   // National format (not accepted)
                "+1234567890",                  // Wrong country code (USA - not supported)
                "+41 21 123",                   // Too short (incomplete)
                "+411234567890123",             // Too long
                "+44 20 7946 0958",             // UK number (not supported)
                "+86 10 1234 5678"              // Chinese number (not supported)
        );
    }
}

