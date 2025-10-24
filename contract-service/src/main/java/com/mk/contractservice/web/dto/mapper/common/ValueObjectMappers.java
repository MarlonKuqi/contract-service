package com.mk.contractservice.web.dto.mapper.common;

import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.MoneyAmount;
import com.mk.contractservice.domain.valueobject.PersonName;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ValueObjectMappers {
    @Named("toName")
    default PersonName toName(String v) {
        return PersonName.of(v);
    }

    @Named("toEmail")
    default Email toEmail(String v) {
        return Email.of(v);
    }

    @Named("toPhone")
    default PhoneNumber toPhone(String v) {
        return PhoneNumber.of(v);
    }

    @Named("toMoney")
    default MoneyAmount toMoney(BigDecimal v) {
        return MoneyAmount.of(v);
    }
}