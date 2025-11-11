package com.mk.contractservice.domain.contract;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.exception.ContractNotFoundException;
import com.mk.contractservice.domain.exception.ContractNotOwnedByClientException;
import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.ContractCost;
import com.mk.contractservice.domain.valueobject.ContractPeriod;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PersonBirthDate;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContractService - Business Logic Tests")
class ContractServiceTest {

    @Mock
    private ContractRepository contractRepository;

    private ContractService service;

    private Client testClient;

    @BeforeEach
    void setUp() {
        service = new ContractService(contractRepository);
        
        testClient = Person.builder()
                .id(UUID.randomUUID())
                .name(ClientName.of("John Doe"))
                .email(Email.of("john@example.com"))
                .phone(PhoneNumber.of("+33123456789"))
                .birthDate(PersonBirthDate.of(LocalDate.of(1990, 1, 15)))
                .build();
    }

    @Nested
    @DisplayName("Get Contract For Client - Business Rules")
    class GetContractForClientTests {

        @Test
        @DisplayName("Should return contract when it exists and belongs to client")
        void shouldReturnContractWhenValid() {
            UUID contractId = UUID.randomUUID();
            
            Contract contract = Contract.builder()
                    .id(contractId)
                    .client(testClient)
                    .period(ContractPeriod.of(LocalDateTime.now(), null))
                    .costAmount(ContractCost.of(new BigDecimal("1000.00")))
                    .build();

            when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

            Contract result = service.getContractForClient(testClient.getId(), contractId);

            assertThat(result).isEqualTo(contract);
            verify(contractRepository).findById(contractId);
        }

        @Test
        @DisplayName("Should throw ContractNotFoundException when contract does not exist")
        void shouldThrowWhenContractNotFound() {
            UUID clientId = UUID.randomUUID();
            UUID contractId = UUID.randomUUID();

            when(contractRepository.findById(contractId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getContractForClient(clientId, contractId))
                    .isInstanceOf(ContractNotFoundException.class)
                    .hasMessageContaining(contractId.toString());

            verify(contractRepository).findById(contractId);
        }

        @Test
        @DisplayName("Should throw ContractNotOwnedByClientException when contract belongs to different client")
        void shouldThrowWhenContractBelongsToDifferentClient() {
            UUID differentClientId = UUID.randomUUID();
            UUID contractId = UUID.randomUUID();
            
            Client owner = Person.builder()
                    .id(UUID.randomUUID())
                    .name(ClientName.of("Owner"))
                    .email(Email.of("owner@example.com"))
                    .phone(PhoneNumber.of("+33111111111"))
                    .birthDate(PersonBirthDate.of(LocalDate.of(1980, 1, 1)))
                    .build();

            Contract contract = Contract.builder()
                    .id(contractId)
                    .client(owner)
                    .period(ContractPeriod.of(LocalDateTime.now(), null))
                    .costAmount(ContractCost.of(new BigDecimal("1000.00")))
                    .build();

            when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

            assertThatThrownBy(() -> service.getContractForClient(differentClientId, contractId))
                    .isInstanceOf(ContractNotOwnedByClientException.class)
                    .hasMessageContaining(contractId.toString())
                    .hasMessageContaining(differentClientId.toString());

            verify(contractRepository).findById(contractId);
        }
    }

    @Nested
    @DisplayName("Ensure Contract Belongs To Client - Business Rule")
    class EnsureContractBelongsToClientTests {

        @Test
        @DisplayName("Should not throw when contract belongs to client")
        void shouldNotThrowWhenContractBelongsToClient() {
            UUID contractId = UUID.randomUUID();
            
            Contract contract = Contract.builder()
                    .id(contractId)
                    .client(testClient)
                    .period(ContractPeriod.of(LocalDateTime.now(), null))
                    .costAmount(ContractCost.of(new BigDecimal("1000.00")))
                    .build();

            service.ensureContractBelongsToClient(contract, testClient.getId());
        }

        @Test
        @DisplayName("Should throw when contract belongs to different client")
        void shouldThrowWhenContractBelongsToDifferentClient() {
            UUID differentClientId = UUID.randomUUID();
            UUID contractId = UUID.randomUUID();

            Contract contract = Contract.builder()
                    .id(contractId)
                    .client(testClient)
                    .period(ContractPeriod.of(LocalDateTime.now(), null))
                    .costAmount(ContractCost.of(new BigDecimal("1000.00")))
                    .build();

            assertThatThrownBy(() -> service.ensureContractBelongsToClient(contract, differentClientId))
                    .isInstanceOf(ContractNotOwnedByClientException.class)
                    .hasMessageContaining(contractId.toString())
                    .hasMessageContaining(differentClientId.toString());
        }
    }
}

