package com.medvedev.vegatest.order;

import lombok.Data;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

@Data
public class Order {
    private String id;
    private String financialInstrumentId;
    private String traderId;
    private BigDecimal price;
    private AtomicReference<BigDecimal> quantity;
    private Type type;

    public Order(String id, String financialInstrumentId, String traderId, BigDecimal price, BigDecimal quantity, Type type) {
        this.id = id;
        this.financialInstrumentId = financialInstrumentId;
        this.traderId = traderId;
        this.price = price;
        this.quantity = new AtomicReference<>(quantity);
        this.type = type;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = new AtomicReference<>(quantity);
    }

    public BigDecimal getQuantity() {
        return quantity == null ? null : quantity.get();
    }

    public void subtractQuantity(BigDecimal amount) {
        quantity.getAndUpdate(prev -> prev.subtract(amount));
    }


    public enum Type {
        BUY, SELL;

        public Type getOpposite(){
            return this == BUY ? SELL : BUY;
        }
    }
}
