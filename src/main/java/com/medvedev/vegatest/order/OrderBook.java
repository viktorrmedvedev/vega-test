package com.medvedev.vegatest.order;

import com.medvedev.vegatest.exception.DuplicateEntryException;
import com.medvedev.vegatest.financialinstrument.CompositeFinancialInstrument;
import com.medvedev.vegatest.financialinstrument.FinancialInstrument;
import com.medvedev.vegatest.financialinstrument.FinancialInstrumentsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

@Service
@Slf4j
public class OrderBook {
    private final Map<String, ConcurrentSkipListSet<Order>> buyOrders;
    private final Map<String, ConcurrentSkipListSet<Order>> sellOrders;
    private final Map<String, Order> allOrders;
    private final Map<Order.Type, Map<String, ConcurrentSkipListSet<Order>>> orderBookPerType;
    private final Map<Order.Type, Comparator<Order>> orderBookSortingPerType;
    private final OrderValidator validator;
    private final FinancialInstrumentsService financialInstrumentsService;

    public OrderBook(OrderValidator validator, FinancialInstrumentsService financialInstrumentsService) {
        this.validator = validator;
        this.financialInstrumentsService = financialInstrumentsService;

        this.buyOrders = new ConcurrentHashMap<>();
        this.sellOrders = new ConcurrentHashMap<>();
        this.allOrders = new ConcurrentHashMap<>();

        this.orderBookPerType = Map.of(
                Order.Type.BUY, buyOrders,
                Order.Type.SELL, sellOrders
        );

        this.orderBookSortingPerType = Map.of(
                Order.Type.BUY, Comparator.comparing(Order::getPrice, Comparator.nullsLast(BigDecimal::compareTo)).reversed(),
                Order.Type.SELL, Comparator.comparing(Order::getPrice, Comparator.nullsFirst(BigDecimal::compareTo))
        );
    }

    public void addOrder(Order order) {
        final var orderId = order.getId();

        allOrders.compute(orderId, (key, value) -> {
            if (value != null) {
                throw new DuplicateEntryException("orderId=%s already exists".formatted(orderId));
            }
            validator.validate(order);
            return order;
        });

        final var instrumentId = order.getFinancialInstrumentId();

        orderBookPerType.get(order.getType())
                .computeIfAbsent(
                        instrumentId,
                        k -> new ConcurrentSkipListSet<>(orderBookSortingPerType.get(order.getType())))
                .add(order);
        log.info("Created new order: {}", order);
        updateFinancialInstrument(instrumentId);
        processOrderBook(instrumentId);
    }

    public void cancelOrder(String orderId) {
        removeOrder(orderId);
    }

    // can be also configured as scheduled job
    public void processOrderBook(String instrumentId) {
        while (canProcess(instrumentId)) {
            final var buyQueue = buyOrders.get(instrumentId);
            final var sellQueue = sellOrders.get(instrumentId);
            final var buyOrder = buyQueue.first();
            if (isComposite(buyOrder.getFinancialInstrumentId())) {
                processCompositeOrder(buyOrder);
                return;
            }
            final var sellOrder = sellQueue.first();
            if (isComposite(sellOrder.getFinancialInstrumentId())) {
                processCompositeOrder(sellOrder);
                return;
            }

            if (buyOrder.getPrice() == null || buyOrder.getPrice().compareTo(sellOrder.getPrice()) >= 0) {
                executeTrade(buyOrder, sellOrder, buyOrder.getQuantity().min(sellOrder.getQuantity()));
            } else {
                return;
            }
        }
    }

    private boolean canProcess(String instrumentId) {
        final var buyQueue = buyOrders.get(instrumentId);
        final var sellQueue = sellOrders.get(instrumentId);
        if (isComposite(instrumentId)) {
            return CollectionUtils.isNotEmpty(sellQueue) || CollectionUtils.isNotEmpty(buyQueue);
        }
        return CollectionUtils.isNotEmpty(sellQueue) && CollectionUtils.isNotEmpty(buyQueue);
    }

    private void processCompositeOrder(Order compositeOrder) {
        final var oppositeOrderBook = orderBookPerType.get(compositeOrder.getType().getOpposite());
        final var financialInstrument = (CompositeFinancialInstrument) financialInstrumentsService.get(compositeOrder.getFinancialInstrumentId());

        final var underlyingFinancialInstrumentsQueues = financialInstrument.getChildInstruments()
                .stream()
                .map(FinancialInstrument::getId)
                .map(oppositeOrderBook::get)
                .toList();

        while (underlyingFinancialInstrumentsQueues.stream().noneMatch(ConcurrentSkipListSet::isEmpty)) {
            final var singleOppositeOrders = underlyingFinancialInstrumentsQueues.stream()
                    .map(ConcurrentSkipListSet::first)
                    .toList();

            final var minQuantity = singleOppositeOrders
                    .stream()
                    .map(Order::getQuantity)
                    .reduce(BigDecimal::min)
                    .orElseThrow();

            if (compositeOrderConditionMatches(compositeOrder, singleOppositeOrders)) {
                executeCompositeTrade(compositeOrder, singleOppositeOrders, minQuantity);
            } else {
                return;
            }
        }
    }

    private boolean compositeOrderConditionMatches(Order compositeOrder, List<Order> singleOppositeOrders) {
        if (compositeOrder.getPrice() == null) {
            return true; // assume that user wants to buy for any price
        }
        if (compositeOrder.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }

        final var singleOppositeOrdersCommonPrice = singleOppositeOrders
                .stream()
                .map(Order::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        final var priceComparisonResult = compositeOrder.getPrice().compareTo(singleOppositeOrdersCommonPrice);
        return compositeOrder.getType() == Order.Type.BUY
                ? priceComparisonResult >= 0
                : priceComparisonResult <= 0;
    }

    private void executeTrade(Order buyOrder, Order sellOrder, BigDecimal quantity) {
        buyOrder.subtractQuantity(quantity); // for simplicity I just subtract quantities
        sellOrder.subtractQuantity(quantity);

        cleanup(buyOrder);
        cleanup(sellOrder);

        updateFinancialInstrument(buyOrder.getFinancialInstrumentId());

        log.info("Executed trade: buyOrder={}, sellOrder={}", buyOrder, sellOrder);
    }

    private void executeCompositeTrade(Order compositeOrder, List<Order> singleOrders, BigDecimal quantity) {
        compositeOrder.subtractQuantity(quantity);
        singleOrders.forEach(order -> order.subtractQuantity(quantity));

        cleanup(compositeOrder);
        singleOrders.forEach(this::cleanup);

        updateFinancialInstrument(compositeOrder.getFinancialInstrumentId());
        singleOrders.forEach(order -> updateFinancialInstrument(order.getFinancialInstrumentId()));

        log.info("Executed composite trade: compositeOrder={}, singleOrders={}", compositeOrder, singleOrders);
    }

    private boolean isComposite(String financialInstrumentId) {
        return financialInstrumentsService.get(financialInstrumentId) instanceof CompositeFinancialInstrument;
    }

    private void cleanup(Order order) {
        if (order.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            removeOrder(order.getId());
        }
    }

    private void updateFinancialInstrument(String financialInstrumentId) {
        final var instrumentBuyOrders = buyOrders.get(financialInstrumentId);
        final var instrumentSellOrders = sellOrders.get(financialInstrumentId);
        var buyPrice = determinePrice(instrumentBuyOrders, instrumentSellOrders);
        var sellPrice = determinePrice(instrumentSellOrders, instrumentBuyOrders);

        var newPrice = buyPrice.add(sellPrice).divide(BigDecimal.valueOf(2), RoundingMode.HALF_EVEN);
        financialInstrumentsService.updatePrice(financialInstrumentId, newPrice);
    }

    private BigDecimal determinePrice(ConcurrentSkipListSet<Order> instrumentOrders, ConcurrentSkipListSet<Order> instrumentOppositeOrders) {
        if (!CollectionUtils.isEmpty(instrumentOrders)) {
            if (CollectionUtils.isEmpty(instrumentOppositeOrders)) {
                return ObjectUtils.firstNonNull(instrumentOrders.first().getPrice(), BigDecimal.ZERO);
            }
            return ObjectUtils.firstNonNull(instrumentOrders.first().getPrice(), instrumentOppositeOrders.first().getPrice(), BigDecimal.ZERO);
        }
        if (CollectionUtils.isEmpty(instrumentOppositeOrders)) {
            return BigDecimal.ZERO;
        }
        return ObjectUtils.firstNonNull(instrumentOppositeOrders.first().getPrice(), BigDecimal.ZERO);

    }

    private void removeOrder(String orderId) {
        final var order = allOrders.remove(orderId);
        if (order != null) {
            orderBookPerType
                    .get(order.getType())
                    .computeIfPresent(order.getFinancialInstrumentId(), (k, v) -> {
                        v.remove(order);
                        return v;
                    });
        }

    }

    // Helper method for testing: Adds an order directly without processing
    public void addOrderWithoutProcessing(Order order) {
        allOrders.put(order.getId(), order);
        orderBookPerType.get(order.getType())
                .computeIfAbsent(order.getFinancialInstrumentId(), k -> new ConcurrentSkipListSet<>(orderBookSortingPerType.get(order.getType())))
                .add(order);
        updateFinancialInstrument(order.getFinancialInstrumentId());
    }

    // Helper method for testing: Checks if an order exists in the book
    public boolean containsOrder(Order order) {
        return allOrders.containsKey(order.getId());
    }
}
