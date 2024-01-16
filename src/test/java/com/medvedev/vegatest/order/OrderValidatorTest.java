package com.medvedev.vegatest.order;

import com.medvedev.vegatest.financialinstrument.FinancialInstrument;
import com.medvedev.vegatest.financialinstrument.FinancialInstrumentValidator;
import com.medvedev.vegatest.financialinstrument.FinancialInstrumentsProperties;
import com.medvedev.vegatest.financialinstrument.FinancialInstrumentsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderValidatorTest {
    private final FinancialInstrumentsProperties properties = new FinancialInstrumentsProperties(
            Map.of(
                    "AAPL_ID", "AAPL"
            ),
            Set.of(new FinancialInstrument().setId("AAPL_ID")),
            Set.of()
    );

    private final FinancialInstrumentsService financialInstrumentsService = new FinancialInstrumentsService(properties);
    private final OrderValidator orderValidator = new OrderValidator(financialInstrumentsService);

    @ParameterizedTest
    @MethodSource("validationTestCases")
    void shouldThrowExceptionIfInvalid(Order order, String expectedErrorMessage) {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> orderValidator.validate(order));
        assertTrue(exception.getMessage().contains(expectedErrorMessage));
    }

    @Test
    void shouldNotThrowAnyExceptionIfValid() {
        var order = new Order(UUID.randomUUID().toString(), "AAPL_ID", "Trader1", BigDecimal.TEN, BigDecimal.ONE, Order.Type.BUY);
        assertDoesNotThrow(() -> orderValidator.validate(order));
    }

    static Stream<Arguments> validationTestCases() {
        return Stream.of(
                Arguments.of(
                        new Order(null, "AAPL_ID", "Trader1", BigDecimal.TEN, BigDecimal.ONE, Order.Type.BUY),
                        "Order id is missing"
                ),
                Arguments.of(
                        new Order(UUID.randomUUID().toString(), "AAPL_ID", "Trader1", BigDecimal.TEN, BigDecimal.ONE, null),
                        "type is missing"
                ),
                Arguments.of(
                        new Order(UUID.randomUUID().toString(), null, "Trader1", BigDecimal.TEN, BigDecimal.ONE, Order.Type.BUY),
                        "financial instrument id is missing"
                ),
                Arguments.of(
                        new Order(UUID.randomUUID().toString(), "AAPL_ID", "Trader1", BigDecimal.valueOf(-5), BigDecimal.ONE, Order.Type.BUY),
                        "price can not be negative"
                ),
                Arguments.of(
                        new Order(UUID.randomUUID().toString(), "AAPL_ID", "Trader1", BigDecimal.TEN, BigDecimal.ZERO, Order.Type.BUY),
                        "quantity must be positive"
                ),
                Arguments.of(
                        new Order(UUID.randomUUID().toString(), "UnknownInstrument", "Trader1", BigDecimal.TEN, BigDecimal.ONE, Order.Type.BUY),
                        "unknown financialInstrumentId=UnknownInstrument"
                )
        );
    }
}