package com.medvedev.vegatest.order;

import com.medvedev.vegatest.exception.DuplicateEntryException;
import com.medvedev.vegatest.financialinstrument.CompositeFinancialInstrument;
import com.medvedev.vegatest.financialinstrument.FinancialInstrument;
import com.medvedev.vegatest.financialinstrument.FinancialInstrumentsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

public class OrderBookTest {

    private OrderBook orderBook;

    private final FinancialInstrumentsService financialInstrumentsService = Mockito.mock(FinancialInstrumentsService.class);

    private final OrderValidator orderValidator = Mockito.mock(OrderValidator.class);

    @BeforeEach
    void setUp() {
        Mockito.reset(financialInstrumentsService, orderValidator);
        orderBook = new OrderBook(orderValidator, financialInstrumentsService);
    }

    @Test
    void testAddOrder() {
        var order = anOrder("order1", Order.Type.BUY, new BigDecimal("100.00"), new BigDecimal("10"));
        orderBook.addOrderWithoutProcessing(order);
        assertTrue(orderBook.containsOrder(order), "Order should be added to the book");
    }

    @Test
    void testAddDuplicateOrderThrowsException() {
        var order = anOrder("order1", Order.Type.BUY, new BigDecimal("100.00"), new BigDecimal("10"));
        orderBook.addOrderWithoutProcessing(order);
        assertThrows(DuplicateEntryException.class, () -> orderBook.addOrder(order));
    }

    @Test
    void testOrderProcessing() {
        var buyOrder = anOrder("buyOrder1", Order.Type.BUY, new BigDecimal("100.00"), new BigDecimal("5"));
        var sellOrder = anOrder("sellOrder1", Order.Type.SELL, new BigDecimal("95.00"), new BigDecimal("5"));

        orderBook.addOrderWithoutProcessing(buyOrder);
        orderBook.addOrderWithoutProcessing(sellOrder);

        // Process the order book and check if the orders are matched
        orderBook.processOrderBook("FI123");
        assertFalse(orderBook.containsOrder(buyOrder), "Buy order should be matched and removed");
        assertFalse(orderBook.containsOrder(sellOrder), "Sell order should be matched and removed");
    }

    @Test
    void testExecuteTrade() {
        var buyOrder = anOrder("buyOrder2", Order.Type.BUY, new BigDecimal("100.00"), new BigDecimal("10"));
        var sellOrder = anOrder("sellOrder2", Order.Type.SELL, new BigDecimal("100.00"), new BigDecimal("5"));

        orderBook.addOrderWithoutProcessing(buyOrder);
        orderBook.addOrder(sellOrder);

        assertEquals(0, sellOrder.getQuantity().compareTo(BigDecimal.ZERO), "Sell order quantity should be zero after trade");
        assertEquals(0, buyOrder.getQuantity().compareTo(new BigDecimal("5")), "Buy order quantity should be reduced by the traded amount");
    }

    @Test
    void testExecuteTradeWithoutIndicatedBuyPrice() {
        var buyOrder = anOrder("buyOrder2", Order.Type.BUY, null, new BigDecimal("10"));
        var sellOrder = anOrder("sellOrder2", Order.Type.SELL, new BigDecimal("100.00"), new BigDecimal("5"));

        orderBook.addOrderWithoutProcessing(buyOrder);
        orderBook.addOrder(sellOrder);

        assertEquals(0, sellOrder.getQuantity().compareTo(BigDecimal.ZERO), "Sell order quantity should be zero after trade");
        assertEquals(0, buyOrder.getQuantity().compareTo(new BigDecimal("5")), "Buy order quantity should be reduced by the traded amount");
    }

    @Test
    void testExecuteTradeWithoutIndicatedSellPrice() {
        var buyOrder = anOrder("buyOrder2", Order.Type.BUY, new BigDecimal("100.00"), new BigDecimal("10"));
        var sellOrder = anOrder("sellOrder2", Order.Type.SELL, null, new BigDecimal("5"));

        orderBook.addOrderWithoutProcessing(buyOrder);
        orderBook.addOrder(sellOrder);

        assertEquals(0, sellOrder.getQuantity().compareTo(BigDecimal.ZERO), "Sell order quantity should be zero after trade");
        assertEquals(0, buyOrder.getQuantity().compareTo(new BigDecimal("5")), "Buy order quantity should be reduced by the traded amount");
    }

    @Test
    void testCancelOrder() {
        var order = anOrder("order3", Order.Type.BUY, new BigDecimal("100.00"), new BigDecimal("10"));
        orderBook.addOrderWithoutProcessing(order);
        orderBook.cancelOrder(order.getId());
        assertFalse(orderBook.containsOrder(order), "Order should be removed after cancellation");
    }

    @Test
    void testInvalidOrderThrowsException() {
        var invalidOrder = anOrder("invalidOrder", Order.Type.BUY, new BigDecimal("-100.00"), new BigDecimal("10"));
        doThrow(IllegalStateException.class).when(orderValidator).validate(invalidOrder);
        assertThrows(IllegalStateException.class, () -> orderBook.addOrder(invalidOrder), "Adding an order with negative price should throw an exception");
    }

    @Test
    void testCompositeOrderProcessing() {
        CompositeFinancialInstrument compositeInstrument = aCompositeFinancialInstrument("compositeFI", List.of("FI123", "FI124"));
        when(financialInstrumentsService.get("compositeFI")).thenReturn(compositeInstrument);

        var compositeBuyOrder = anOrder("compositeBuyOrder", Order.Type.BUY, new BigDecimal("200.00"), new BigDecimal("10"));
        compositeBuyOrder.setFinancialInstrumentId("compositeFI");

        var sellOrderFI123 = anOrder("sellOrderFI123", Order.Type.SELL, new BigDecimal("100.00"), new BigDecimal("10"));
        sellOrderFI123.setFinancialInstrumentId("FI123");

        var sellOrderFI124 = anOrder("sellOrderFI124", Order.Type.SELL, new BigDecimal("100.00"), new BigDecimal("10"));
        sellOrderFI124.setFinancialInstrumentId("FI124");

        orderBook.addOrderWithoutProcessing(sellOrderFI123);
        orderBook.addOrderWithoutProcessing(sellOrderFI124);

        orderBook.addOrder(compositeBuyOrder);

        assertFalse(orderBook.containsOrder(compositeBuyOrder), "Composite Buy order should be matched and removed");
        assertFalse(orderBook.containsOrder(sellOrderFI123), "Sell order for FI123 should be matched and removed");
        assertFalse(orderBook.containsOrder(sellOrderFI124), "Sell order for FI124 should be matched and removed");
    }

    @Test
    void testCompositeOrderProcessingWhenChildInstrumentUpdates() {
        CompositeFinancialInstrument compositeInstrument = aCompositeFinancialInstrument("compositeFI", List.of("FI123", "FI124"));
        when(financialInstrumentsService.get("compositeFI")).thenReturn(compositeInstrument);
        when(financialInstrumentsService.findDependentCompositeInstruments("FI124")).thenReturn(Set.of(compositeInstrument.getId()));

        var compositeBuyOrder = anOrder("compositeBuyOrder", Order.Type.BUY, new BigDecimal("200.00"), new BigDecimal("10"));
        compositeBuyOrder.setFinancialInstrumentId("compositeFI");

        var sellOrderFI123 = anOrder("sellOrderFI123", Order.Type.SELL, new BigDecimal("100.00"), new BigDecimal("10"));
        sellOrderFI123.setFinancialInstrumentId("FI123");

        var sellOrderFI124 = anOrder("sellOrderFI124", Order.Type.SELL, new BigDecimal("100.00"), new BigDecimal("10"));
        sellOrderFI124.setFinancialInstrumentId("FI124");

        orderBook.addOrderWithoutProcessing(sellOrderFI123);
        orderBook.addOrderWithoutProcessing(compositeBuyOrder);

        orderBook.addOrder(sellOrderFI124);

        assertFalse(orderBook.containsOrder(compositeBuyOrder), "Composite Buy order should be matched and removed");
        assertFalse(orderBook.containsOrder(sellOrderFI123), "Sell order for FI123 should be matched and removed");
        assertFalse(orderBook.containsOrder(sellOrderFI124), "Sell order for FI124 should be matched and removed");
    }


    private Order anOrder(String id, Order.Type type, BigDecimal price, BigDecimal quantity) {
        return new Order(id, "FI123", "Trader123", price, quantity, type);
    }


    private CompositeFinancialInstrument aCompositeFinancialInstrument(String id, List<String> childInstrumentIds) {
        Set<FinancialInstrument> childInstruments = childInstrumentIds.stream()
                .map(fiId -> new FinancialInstrument(fiId, "Symbol" + fiId, BigDecimal.ZERO))
                .collect(Collectors.toSet());
        return new CompositeFinancialInstrument(id, "CompositeSymbol", BigDecimal.ZERO, childInstruments);
    }

}