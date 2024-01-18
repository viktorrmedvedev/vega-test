package com.medvedev.vegatest.financialinstrument;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class FinancialInstrumentsService {
    private final Map<String, FinancialInstrument> financialInstruments;
    private final Map<String, Set<String>> compositeFinancialInstrumentsPerSimpleInstrumentId;

    public FinancialInstrumentsService(FinancialInstrumentsProperties financialInstrumentsProperties) {
        this.financialInstruments = new ConcurrentHashMap<>();
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

        compositeFinancialInstrumentsPerSimpleInstrumentId = Collections.unmodifiableMap(
                financialInstrumentsProperties
                .simpleInstruments()
                .stream()
                .collect(Collectors.toMap(
                        FinancialInstrument::getId,
                        key -> findCompositeIdsForSimpleInstrument(key, financialInstrumentsProperties.compositeInstruments()),
                        (existing, replacement) -> existing,
                        HashMap::new))
        );
    }

    public void put(FinancialInstrument instrument) {
        FinancialInstrumentValidator.validate(instrument);
        financialInstruments.put(instrument.getId(), instrument);
    }

    public FinancialInstrument get(String id) {
        return financialInstruments.get(id);
    }

    public void updatePrice(String id, BigDecimal price) {
        financialInstruments.computeIfPresent(id, (k, v) -> v.setPrice(price));
    }

    public Set<String> findDependentCompositeInstruments(String id) {
        return compositeFinancialInstrumentsPerSimpleInstrumentId.getOrDefault(id, Set.of());
    }

    private Set<String> findCompositeIdsForSimpleInstrument(FinancialInstrument simpleInstrument, Set<CompositeFinancialInstrument> compositeFinancialInstruments) {
        return compositeFinancialInstruments.stream()
                .filter(composite -> composite.getChildInstruments().stream()
                        .anyMatch(child -> child.getId().equals(simpleInstrument.getId())))
                .map(CompositeFinancialInstrument::getId)
                .collect(Collectors.toSet());
    }
}
