package th.co.chaiyo.customerportal.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;
import th.co.chaiyo.customerportal.adaptor.Customer360Adapter;
import th.co.chaiyo.customerportal.adaptor.Customer360ProfileDto;
import th.co.chaiyo.customerportal.exception.CustomerNotFoundException;
import th.co.chaiyo.customerportal.model.response.ProfileResponse;

/**
 * Boots the real application context - router, CORS filter, controller and
 * service all wired together - and stubs only Customer360Adapter, the one
 * external boundary the test runner genuinely cannot stand up.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestPropertySource(properties = "cors.allowed-origin-patterns=https://portal.chaiyo.co.th")
class CustomerProfileIntegrationTest {

    private static final String PORTAL_ORIGIN = "https://portal.chaiyo.co.th";

    // WebTestClient here is bound in-process to the application context (no socket),
    // so requests default to a relative URI with no scheme. CorsUtils.isSameOrigin
    // needs the request's own scheme to compare against Origin, so the two CORS
    // tests below rebind onto an absolute base URL - everything else about the
    // filter, router and controller wiring stays real.
    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private Customer360Adapter customer360Adapter;

    private Customer360ProfileDto sampleDto() {
        return new Customer360ProfileDto(
                "0812345678", "123 Moo 4", "Soi 5", "Bang Rak", "Bang Rak", "Bangkok", "10500");
    }

    @Test
    void getProfileReturnsAllSevenFieldsPlusAVersionThroughTheFullStack() {
        when(customer360Adapter.fetchProfile("cust-1")).thenReturn(Mono.just(sampleDto()));

        ProfileResponse response = webTestClient.get().uri("/v1/customers/cust-1/profile")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProfileResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response.phone()).isEqualTo("0812345678");
        assertThat(response.addressLine1()).isEqualTo("123 Moo 4");
        assertThat(response.addressLine2()).isEqualTo("Soi 5");
        assertThat(response.subDistrict()).isEqualTo("Bang Rak");
        assertThat(response.district()).isEqualTo("Bang Rak");
        assertThat(response.province()).isEqualTo("Bangkok");
        assertThat(response.postalCode()).isEqualTo("10500");
        assertThat(response.version()).isNotBlank();
    }

    @Test
    void twoConsecutiveReadsWithNoInterveningWriteReturnTheSameVersion() {
        when(customer360Adapter.fetchProfile("cust-1"))
                .thenReturn(Mono.just(sampleDto()), Mono.just(sampleDto()));

        String firstVersion = readProfile("cust-1").version();
        String secondVersion = readProfile("cust-1").version();

        assertThat(secondVersion).isEqualTo(firstVersion);
    }

    @Test
    void crossOriginGetFromThePortalOriginIsPermittedAndReachesTheService() {
        when(customer360Adapter.fetchProfile("cust-1")).thenReturn(Mono.just(sampleDto()));

        crossOriginClient().get().uri("/v1/customers/cust-1/profile")
                .header(HttpHeaders.ORIGIN, PORTAL_ORIGIN)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, PORTAL_ORIGIN)
                .expectBody()
                .jsonPath("$.phone").isEqualTo("0812345678")
                .jsonPath("$.version").exists();
    }

    @Test
    void crossOriginGetFromAnUnlistedOriginIsRejectedBeforeReachingTheService() {
        crossOriginClient().get().uri("/v1/customers/cust-1/profile")
                .header(HttpHeaders.ORIGIN, "https://evil.example.com")
                .exchange()
                .expectStatus().isForbidden();
    }

    private WebTestClient crossOriginClient() {
        return webTestClient.mutate().baseUrl("https://customer-portal-service.chaiyo.co.th").build();
    }

    @Test
    void getProfilePropagatesACustomerNotFoundFromCustomer360AsA404ThroughTheFullStack() {
        when(customer360Adapter.fetchProfile("missing"))
                .thenReturn(Mono.error(new CustomerNotFoundException("missing")));

        webTestClient.get().uri("/v1/customers/missing/profile")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("NF001");
    }

    private ProfileResponse readProfile(String customerId) {
        return webTestClient.get().uri("/v1/customers/{id}/profile", customerId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProfileResponse.class)
                .returnResult()
                .getResponseBody();
    }
}
