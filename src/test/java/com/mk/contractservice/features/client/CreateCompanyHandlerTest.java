package com.mk.contractservice.features.client;


import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.ClientValidationService;
import com.mk.contractservice.domain.client.exception.CompanyIdentifierAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateCompany Handler")
class CreateCompanyHandlerTest {

    @Mock
    private ClientRepository clientRepository;

    private ClientValidationService clientValidationService;
    private CreateCompany.Handler createCompanyHandler;

    @BeforeEach
    void setUp() {
        clientValidationService = new ClientValidationService(clientRepository);
        createCompanyHandler = new CreateCompany.Handler(clientRepository, clientValidationService);
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
                .isInstanceOf(CompanyIdentifierAlreadyExistsException.class);
    }
}

