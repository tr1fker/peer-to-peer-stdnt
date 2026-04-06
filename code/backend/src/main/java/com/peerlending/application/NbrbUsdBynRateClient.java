package com.peerlending.application;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;

/**
 * Official USD rate vs Belarusian ruble (BYN) from the National Bank of the Republic of Belarus.
 */
@Component
public class NbrbUsdBynRateClient {

    /** USD in NBRB exrates API (see https://api.nbrb.by/exrates/rates/431). */
    private static final int USD_CUR_ID = 431;

    private final RestClient restClient;

    public NbrbUsdBynRateClient(RestClient.Builder builder) {
        this.restClient = builder.baseUrl("https://api.nbrb.by").build();
    }

    @Cacheable(cacheNames = "fxRates", key = "'USD-BYN'")
    public Optional<BigDecimal> latestUsdToByn() {
        try {
            Map<?, ?> body = restClient.get()
                    .uri("/exrates/rates/{id}", USD_CUR_ID)
                    .retrieve()
                    .body(Map.class);
            if (body == null || !body.containsKey("Cur_OfficialRate") || !body.containsKey("Cur_Scale")) {
                return Optional.empty();
            }
            BigDecimal official = new BigDecimal(body.get("Cur_OfficialRate").toString());
            int scale = ((Number) body.get("Cur_Scale")).intValue();
            if (scale <= 0) {
                return Optional.empty();
            }
            BigDecimal perUnit = official.divide(BigDecimal.valueOf(scale), 6, RoundingMode.HALF_UP);
            return Optional.of(perUnit.stripTrailingZeros());
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
}
