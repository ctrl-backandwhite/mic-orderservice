package com.backandwhite.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.backandwhite.common.domain.valueobject.Money;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MoneyMapperHelperTest {

    private MoneyMapperHelper mapper;

    @BeforeEach
    void setUp() {
        mapper = new MoneyMapperHelper();
    }

    @Test
    void moneyToBigDecimal_withValue_returnsAmount() {
        Money money = Money.of(new BigDecimal("12.34"));
        BigDecimal result = mapper.moneyToBigDecimal(money);
        assertThat(result).isEqualByComparingTo("12.34");
    }

    @Test
    void moneyToBigDecimal_withNull_returnsNull() {
        assertThat(mapper.moneyToBigDecimal(null)).isNull();
    }

    @Test
    void bigDecimalToMoney_withValue_returnsMoney() {
        Money result = mapper.bigDecimalToMoney(new BigDecimal("99.99"));
        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualByComparingTo("99.99");
    }

    @Test
    void bigDecimalToMoney_withNull_returnsNull() {
        assertThat(mapper.bigDecimalToMoney(null)).isNull();
    }
}
