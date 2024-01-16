package com.medvedev.vegatest.financialinstrument;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FinancialInstrumentValidatorTest {

    @ParameterizedTest
    @MethodSource("validationTestCases")
    void shouldThrowExceptionIfInvalid(FinancialInstrument instrument, String expectedErrorMessage) {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> FinancialInstrumentValidator.validate(instrument));
        assertTrue(exception.getMessage().contains(expectedErrorMessage));
    }

    @Test
    void shouldNotThrowAnyExceptionIfValid() {
        var financialInstrument = new FinancialInstrument("1", "AAPL_ID", BigDecimal.TEN);
        assertDoesNotThrow(() -> FinancialInstrumentValidator.validate(financialInstrument));
    }

    static Stream<Arguments> validationTestCases() {
        return Stream.of(
                Arguments.of(
                        new FinancialInstrument(null, "AAPL_ID", BigDecimal.TEN),
                        "Financial instrument id is missing"
                ),
                Arguments.of(
                        new FinancialInstrument("1", null, BigDecimal.TEN), "symbol is missing"
                ),
                Arguments.of(
                        new FinancialInstrument("1", "AAPL_ID", BigDecimal.valueOf(-5)),
                        "price must not be null or negative"),
                Arguments.of(
                        new CompositeFinancialInstrument(
                                "1",
                                "Composite",
                                BigDecimal.TEN,
                                Set.of()
                        ),
                        "childInstruments must not be empty"
                ),
                Arguments.of(
                        new CompositeFinancialInstrument(
                                "1",
                                "Composite",
                                BigDecimal.TEN,
                                Set.of(new CompositeFinancialInstrument(
                                        "1",
                                        "Composite",
                                        BigDecimal.TEN,
                                                Set.of(new FinancialInstrument(
                                                        "3",
                                                        "AAPL_ID",
                                                        BigDecimal.TEN
                                                        )))
                                        )
                        ),
                        "Child instruments in composite financialInstrumentId=1 can not be composites themselves"
                )
        );
    }
}
