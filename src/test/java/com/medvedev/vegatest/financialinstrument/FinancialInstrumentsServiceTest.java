package com.medvedev.vegatest.financialinstrument;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FinancialInstrumentsServiceTest {

    private final FinancialInstrumentsProperties properties = new FinancialInstrumentsProperties(
            Map.of(
                    "AAPL_ID", "AAPL"
            ),
            Set.of(new FinancialInstrument().setId("AAPL_ID")),
            Set.of()
    );

    private final FinancialInstrumentsService financialInstrumentsService = new FinancialInstrumentsService(properties);

    @Test
    void shouldPutValidFinancialInstrumentToTheCollection() {
        // given
        var instrument = new FinancialInstrument(UUID.randomUUID().toString(), "AAPL", BigDecimal.TEN);

        // when
        financialInstrumentsService.put(instrument);

        // then
        var retrievedInstrument = financialInstrumentsService.get(instrument.getId());
        assertEquals(instrument, retrievedInstrument);
    }

    @Test
    void shouldThrowExceptionWhenPutInvalidFinancialInstrument() {
        // given
        var invalidInstrument = new FinancialInstrument(null, "AAPL", BigDecimal.TEN);

        // when + then
        assertThrows(IllegalStateException.class, () -> financialInstrumentsService.put(invalidInstrument));
    }

    @Test
    void shouldRetrieveExistingFinancialInstrument() {
        // given
        var instrument = new FinancialInstrument(UUID.randomUUID().toString(), "AAPL", BigDecimal.TEN);
        financialInstrumentsService.put(instrument);

        // when
        var retrievedInstrument = financialInstrumentsService.get(instrument.getId());

        // then
        assertEquals(instrument, retrievedInstrument);
    }

    @Test
    void shouldReturnNullIfTryingToRetrieveNonExistentInstrument() {
        // when
        var retrievedInstrument = financialInstrumentsService.get(UUID.randomUUID().toString());

        // then
        assertNull(retrievedInstrument);
    }
}