package com.mk.contractservice.features.client;


import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.Company;
import com.mk.contractservice.domain.client.CompanyFactory;
import com.mk.contractservice.domain.client.CompanyIdentifierUniqueness;
import com.mk.contractservice.domain.client.exception.CompanyIdentifierAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateCompany Handler")
class CreateCompanyHandlerTest {

    @Mock
    private ClientRepository clientRepository;

    private CompanyIdentifierUniqueness companyIdentifierUniqueness;
    private CreateCompany.Handler createCompanyHandler;

    @BeforeEach
    void setUp() {
        companyIdentifierUniqueness = new CompanyIdentifierUniqueness(clientRepository);
        createCompanyHandler = new CreateCompany.Handler(clientRepository, companyIdentifierUniqueness);
    }

    @Test
    @DisplayName("GIVEN identifiant déjà existant WHEN execute THEN lève CompanyIdentifierAlreadyExistsException")
    void shouldThrowExceptionWhenCompanyIdentifierAlreadyExists() {
        // Given
        String duplicateIdentifier = "123456789";
        CreateCompany.Command command = new CreateCompany.Command(
                "Tech Corp",
                "contact@techcorp.com",
                "+33123456789",
                duplicateIdentifier
        );

        when(clientRepository.existsByCompanyIdentifier(duplicateIdentifier)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> createCompanyHandler.execute(command))
                .isInstanceOf(CompanyIdentifierAlreadyExistsException.class)
                .extracting(ex -> ((CompanyIdentifierAlreadyExistsException) ex).getCompanyIdentifier())
                .isEqualTo(duplicateIdentifier);
    }

    @Test
    @DisplayName("GIVEN identifiant unique WHEN execute THEN sauvegarde et retourne l'entreprise créée")
    void shouldSaveAndReturnCreatedCompany() {
        // Given
        CreateCompany.Command command = new CreateCompany.Command(
                "Tech Corp",
                "contact@techcorp.com",
                "+33123456789",
                "123456789"
        );
        Company saved = CompanyFactory.buildFromDatabase(
                UUID.randomUUID(),
                "Tech Corp",
                "contact@techcorp.com",
                "+33123456789",
                "123456789"
        );
        when(clientRepository.existsByCompanyIdentifier("123456789")).thenReturn(false);
        when(clientRepository.save(any(Company.class))).thenReturn(saved);

        // When
        Company result = createCompanyHandler.execute(command);

        // Then
        assertThat(result).isSameAs(saved);
    }
}

