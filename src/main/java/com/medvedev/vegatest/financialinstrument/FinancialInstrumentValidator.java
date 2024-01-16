package com.medvedev.vegatest.financialinstrument;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;

import static java.math.BigDecimal.ZERO;

public class FinancialInstrumentValidator {

    public static void validate(FinancialInstrument financialInstrument) {
        Validate.validState(financialInstrument.getId() != null,
                "Financial instrument id is missing");

        Validate.validState(financialInstrument.getSymbol() != null,
                "financialInstrumentId=%s symbol is missing".formatted(financialInstrument.getId()));

        Validate.validState(financialInstrument.getPrice() != null && financialInstrument.getPrice().compareTo(ZERO) >= 0,
                "financialInstrumentId=%s price must not be null or negative".formatted(financialInstrument.getId()));

        if (financialInstrument instanceof CompositeFinancialInstrument) {
            final var childInstruments = ((CompositeFinancialInstrument) financialInstrument).getChildInstruments();
            Validate.validState(CollectionUtils.isNotEmpty(childInstruments),
                    "financialInstrumentId=%s childInstruments must not be empty".formatted(financialInstrument.getId()));
            childInstruments.forEach(childInstrument -> {
                if (childInstrument instanceof CompositeFinancialInstrument) {
                    throw new IllegalStateException("Child instruments in composite financialInstrumentId=%s can not be composites themselves".formatted(financialInstrument.getId()));
                }
                validate(childInstrument);
            });
        }
    }
}
