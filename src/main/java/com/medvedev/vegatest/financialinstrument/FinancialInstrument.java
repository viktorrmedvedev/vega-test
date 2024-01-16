package com.medvedev.vegatest.financialinstrument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class FinancialInstrument {
    private String id;
    private String symbol;
    private BigDecimal price = BigDecimal.ZERO;
}
