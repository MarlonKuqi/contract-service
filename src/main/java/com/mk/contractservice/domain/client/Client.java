package com.mk.contractservice.domain.client;

import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "client")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Embedded
    @NotNull(message = "Name must not be null")
    @Valid
    private ClientName name;

    @Embedded
    @NotNull(message = "Email must not be null")
    @Valid
    private Email email;

    @Embedded
    @NotNull(message = "Phone must not be null")
    @Valid
    private PhoneNumber phone;

    protected Client(final ClientName name, final Email email, final PhoneNumber phone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    /**
     * Updates the common fields of a client.
     * Used for PUT /v1/clients/{id} endpoint.
     * birthDate and companyIdentifier are NOT updated (business rule).
     *
     * <p>This method enforces invariants at the domain level, independently of the calling context
     * (Controller, Batch, Tests, etc.). The null checks protect against programming errors
     * and ensure fail-fast behavior with clear error messages.</p>
     *
     * @param name new name (must not be null)
     * @param email new email (must not be null)
     * @param phone new phone number (must not be null)
     * @throws IllegalArgumentException if any parameter is null
     */
    public void updateCommonFields(final ClientName name, final Email email, final PhoneNumber phone) {
        // Defense in depth: validate invariants at domain level
        // This protects against programming errors in ANY calling context
        if (name == null || email == null || phone == null) {
            final String nullFields = Stream.of(
                    name == null ? "name" : null,
                    email == null ? "email" : null,
                    phone == null ? "phone" : null
            ).filter(Objects::nonNull).collect(Collectors.joining(", "));

            throw new IllegalArgumentException(
                    "Cannot update client: the following required fields are null: " + nullFields
            );
        }

        this.name = name;
        this.email = email;
        this.phone = phone;
    }
}
