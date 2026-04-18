package com.backandwhite.api.mapper;

import com.backandwhite.common.domain.valueobject.Money;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * MapStruct helper: converts between {@link Money} (domain) and
 * {@link BigDecimal} (DTO). Referenced via
 * {@code @Mapper(uses = MoneyMapperHelper.class)}.
 */
@Component
public class MoneyMapperHelper {

    public BigDecimal moneyToBigDecimal(Money money) {
        return money == null ? null : money.getAmount();
    }

    public Money bigDecimalToMoney(BigDecimal bd) {
        return bd == null ? null : Money.of(bd);
    }
}
