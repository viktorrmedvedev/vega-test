package com.medvedev.vegatest.financialinstrument;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.Set;

@ConfigurationProperties(prefix = "financial-instruments")
public record FinancialInstrumentsProperties(
        Map<String, String> mapping,
        Set<FinancialInstrument> simpleInstruments,
        Set<CompositeFinancialInstrument> compositeInstruments
) {
}
