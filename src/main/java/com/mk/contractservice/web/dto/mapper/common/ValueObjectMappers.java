package com.mk.contractservice.web.dto.mapper.common;

import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.ContractCost;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PersonBirthDate;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ValueObjectMappers {
    @Named("toName")
    default ClientName toName(String v) {
        return ClientName.of(v);
    }

    @Named("toEmail")
    default Email toEmail(String v) {
        return Email.of(v);
    }

    @Named("toPhone")
    default PhoneNumber toPhone(String v) {
        return PhoneNumber.of(v);
    }

    @Named("toBirthDate")
    default PersonBirthDate toBirthDate(LocalDate v) {
        return PersonBirthDate.of(v);
    }

    @Named("toMoney")
    default ContractCost toMoney(BigDecimal v) {
        return ContractCost.of(v);
    }
}