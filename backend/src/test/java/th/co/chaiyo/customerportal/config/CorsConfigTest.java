package th.co.chaiyo.customerportal.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.cors.reactive.CorsWebFilter;

import reactor.core.publisher.Mono;

class CorsConfigTest {

    private CorsWebFilter filterAllowing(String... allowedOriginPatterns) {
        PortalCorsProperties properties = new PortalCorsProperties();
        properties.setAllowedOriginPatterns(List.of(allowedOriginPatterns));
        return new CorsConfig().corsWebFilter(properties);
    }

    private MockServerWebExchange preflightFrom(String origin) {
        MockServerHttpRequest request = MockServerHttpRequest
                .method(HttpMethod.OPTIONS, URI.create("https://api.chaiyo.co.th/v1/customers/cust-1/profile"))
                .header(HttpHeaders.ORIGIN, origin)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .build();
        return MockServerWebExchange.from(request);
    }

    @Test
    void permitsPreflightFromTheConfiguredPortalOrigin() {
        CorsWebFilter filter = filterAllowing("https://portal.chaiyo.co.th");
        MockServerWebExchange exchange = preflightFrom("https://portal.chaiyo.co.th");

        filter.filter(exchange, ex -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exchange.getResponse().getHeaders().getAccessControlAllowOrigin())
                .isEqualTo("https://portal.chaiyo.co.th");
    }

    @Test
    void rejectsPreflightFromAnOriginOutsideTheAllowList() {
        CorsWebFilter filter = filterAllowing("https://portal.chaiyo.co.th");
        MockServerWebExchange exchange = preflightFrom("https://evil.example.com");

        filter.filter(exchange, ex -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
