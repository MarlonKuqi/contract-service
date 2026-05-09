# Script pour remplacer Person.create par Person.builder dans les tests

## Pattern à remplacer
Person.create(
    ClientName.of("..."),
    Email.of("..."),
    PhoneNumber.of("..."),
    PersonBirthDate.of(...)
)

## Remplacer par
Person.builder()
    .name(ClientName.of("..."))
    .email(Email.of("..."))
    .phone(PhoneNumber.of("..."))
    .birthDate(PersonBirthDate.of(...))
    .build()

