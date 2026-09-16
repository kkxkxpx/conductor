package th.co.chaiyo.customerportal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * R-12: the portal SPA calls this service cross-origin from its deployed origin.
 * Origins are config-driven (cors.allowed-origin-patterns) so each environment
 * can scope access to its own portal deployment without a code change.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter(PortalCorsProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(properties.getAllowedOriginPatterns());
        configuration.setAllowedMethods(java.util.List.of(
                HttpMethod.GET.name(), HttpMethod.POST.name(), HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(), HttpMethod.OPTIONS.name()));
        configuration.setAllowedHeaders(java.util.List.of("*"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/v1/**", configuration);
        return new CorsWebFilter(source);
    }
}
