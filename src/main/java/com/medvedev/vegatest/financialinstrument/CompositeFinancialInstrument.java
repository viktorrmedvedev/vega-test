package com.medvedev.vegatest.financialinstrument;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
public class CompositeFinancialInstrument extends FinancialInstrument {
    private Set<FinancialInstrument> childInstruments;

    public CompositeFinancialInstrument(String id, String symbol, BigDecimal price, Set<FinancialInstrument> childInstruments) {
        super(id, symbol, price);
        this.childInstruments = childInstruments;
    }
}
