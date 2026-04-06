package com.peerlending.api;

import com.peerlending.application.NbrbUsdBynRateClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/rates")
public class RatesController {

    private final NbrbUsdBynRateClient nbrbUsdBynRateClient;

    public RatesController(NbrbUsdBynRateClient nbrbUsdBynRateClient) {
        this.nbrbUsdBynRateClient = nbrbUsdBynRateClient;
    }

    @GetMapping("/usd-byn")
    public Map<String, Object> usdByn() {
        return nbrbUsdBynRateClient.latestUsdToByn()
                .map(rate -> Map.<String, Object>of(
                        "from", "USD",
                        "to", "BYN",
                        "rate", rate
                ))
                .orElse(Map.of("error", "RATE_UNAVAILABLE"));
    }
}
