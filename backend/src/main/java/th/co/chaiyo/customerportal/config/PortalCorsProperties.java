package th.co.chaiyo.customerportal.config;

import java.util.List;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "cors")
public class PortalCorsProperties {

    /**
     * Origin patterns allowed to call this service from the browser, e.g. the
     * portal SPA's deployed origin. Configured per environment (ConfigMap).
     */
    private List<String> allowedOriginPatterns = List.of("http://localhost:3000");
}
