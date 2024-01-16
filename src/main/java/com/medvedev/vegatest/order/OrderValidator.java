package com.medvedev.vegatest.order;

import com.medvedev.vegatest.financialinstrument.FinancialInstrumentsService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Component;

import static java.math.BigDecimal.ZERO;

@Component
@RequiredArgsConstructor
public class OrderValidator {
    private final FinancialInstrumentsService financialInstrumentsService;

    public void validate(Order order) {
        Validate.validState(order.getId() != null,
                "Order id is missing");

        Validate.validState(order.getType() != null,
                "orderId=%s type is missing".formatted(order.getId()));

        Validate.validState(StringUtils.isNotBlank(order.getFinancialInstrumentId()),
                "orderId=%s financial instrument id is missing".formatted(order.getId()));

        Validate.validState(financialInstrumentsService.get(order.getFinancialInstrumentId()) != null,
                "orderId=%s unknown financialInstrumentId=%s".formatted(order.getId(), order.getFinancialInstrumentId()));

        Validate.validState(order.getPrice() == null || order.getPrice().compareTo(ZERO) >= 0,
                "orderId=%s price can not be negative".formatted(order.getId()));

        Validate.validState(order.getQuantity() != null && order.getQuantity().compareTo(ZERO) > 0,
                "orderId=%s quantity must be positive".formatted(order.getId()));
    }
}
