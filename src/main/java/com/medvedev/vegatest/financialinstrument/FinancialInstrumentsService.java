package com.medvedev.vegatest.financialinstrument;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FinancialInstrumentsService {
    private final Map<String, FinancialInstrument> financialInstruments;

    public FinancialInstrumentsService(FinancialInstrumentsProperties financialInstrumentsProperties) {
        this.financialInstruments = new ConcurrentHashMap<>();
        initFinancialInstruments(financialInstrumentsProperties);
    }

    public void put(FinancialInstrument instrument) {
        FinancialInstrumentValidator.validate(instrument);
        financialInstruments.put(instrument.getId(), instrument);
    }

    public FinancialInstrument get(String id) {
        return financialInstruments.get(id);
    }

    private void initFinancialInstruments(FinancialInstrumentsProperties financialInstrumentsProperties) {
        final var mapping = financialInstrumentsProperties.mapping();

        financialInstrumentsProperties
                .simpleInstruments()
                .stream()
                .peek(instrument -> instrument.setSymbol(mapping.get(instrument.getId())))
                .peek(instrument -> instrument.setPrice(BigDecimal.ZERO))
                .peek(FinancialInstrumentValidator::validate)
                .forEach(i -> financialInstruments.put(i.getId(), i));

        financialInstrumentsProperties
                .compositeInstruments()
                .stream()
                .peek(instrument -> instrument.setSymbol(mapping.get(instrument.getId())))
                .peek(instrument -> instrument.setPrice(BigDecimal.ZERO))
                .peek(instrument -> instrument.getChildInstruments().forEach(child -> child.setSymbol(mapping.get(child.getId()))))
                .peek(FinancialInstrumentValidator::validate)
                .forEach(i -> financialInstruments.put(i.getId(), i));
    }
}
