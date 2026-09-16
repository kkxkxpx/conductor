package th.co.chaiyo.customerportal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "customer360")
public class Customer360Properties {

    /**
     * Base URL of the Customer360 API. Injected via Kubernetes ConfigMap per environment.
     */
    private String baseUrl;

    /**
     * Injected at runtime from Vault (chaiyo-vault-helper) — never committed.
     */
    private String apiKey;
    private String apiSecret;

    private int connectTimeoutMs = 3000;
    private int responseTimeoutMs = 5000;
}
