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
        if (name == null) {
            throw new IllegalArgumentException("Name must not be null");
        }
        if (email == null) {
            throw new IllegalArgumentException("Email must not be null");
        }
        if (phone == null) {
            throw new IllegalArgumentException("Phone must not be null");
        }
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    public void updateCommonFields(final ClientName name, final Email email, final PhoneNumber phone) {
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
