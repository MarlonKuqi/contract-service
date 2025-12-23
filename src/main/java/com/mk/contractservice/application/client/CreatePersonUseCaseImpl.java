package com.mk.contractservice.application.client;

import com.mk.contractservice.application.client.usecase.CreatePersonUseCase;
import com.mk.contractservice.domain.client.aggregate.Person;
import com.mk.contractservice.domain.client.repository.ClientRepository;
import com.mk.contractservice.domain.client.service.ClientValidationService;
import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.ClientName;
import com.mk.contractservice.domain.client.valueobject.ClientPhoneNumber;
import com.mk.contractservice.domain.client.valueobject.PersonBirthDate;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class CreatePersonUseCaseImpl implements CreatePersonUseCase {

    ClientRepository clientRepository;
    ClientValidationService clientValidationService;

    @Override
    public Person execute(CreatePersonCommand command) {
        final ClientName name = ClientName.of(command.name());
        final ClientEmail email = ClientEmail.of(command.email());
        final ClientPhoneNumber phone = ClientPhoneNumber.of(command.phoneNumber());
        final PersonBirthDate birthDate = PersonBirthDate.of(command.birthDate());

        clientValidationService.ensureEmailIsUnique(email);

        final Person person = Person.of(name, email, phone, birthDate);
        return (Person) clientRepository.save(person);
    }
}
