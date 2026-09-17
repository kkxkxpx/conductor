package th.co.chaiyo.customerportal.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import th.co.chaiyo.customerportal.adaptor.Customer360AuditRecordDto;
import th.co.chaiyo.customerportal.adaptor.Customer360ProfileDto;
import th.co.chaiyo.customerportal.exception.AuditWriteFailedException;
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

    @BeforeEach
    void stubAuditWritesAsSuccessful() {
        lenient().when(customer360Adapter.appendAuditRecord(any())).thenReturn(Mono.empty());
    }

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

    @Test
    void patchProfileReturns200WithEveryFieldAtItsNewValueThroughTheFullStack() {
        when(customer360Adapter.fetchProfile("cust-1")).thenReturn(Mono.just(sampleDto()));
        Customer360ProfileDto updatedDto = new Customer360ProfileDto(
                "0812345678", "999 Moo 9", "Soi 9", "Silom", "Bang Rak", "Bangkok", "10500");
        when(customer360Adapter.updateProfile(eq("cust-1"), any())).thenReturn(Mono.just(updatedDto));
        String currentVersion = readProfile("cust-1").version();

        webTestClient.patch().uri("/v1/customers/cust-1/profile")
                .header(HttpHeaders.IF_MATCH, currentVersion)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"addressLine1\":\"999 Moo 9\",\"addressLine2\":\"Soi 9\",\"subDistrict\":\"Silom\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.phone").isEqualTo("0812345678")
                .jsonPath("$.addressLine1").isEqualTo("999 Moo 9")
                .jsonPath("$.addressLine2").isEqualTo("Soi 9")
                .jsonPath("$.subDistrict").isEqualTo("Silom")
                .jsonPath("$.district").isEqualTo("Bang Rak")
                .jsonPath("$.province").isEqualTo("Bangkok")
                .jsonPath("$.postalCode").isEqualTo("10500");
    }

    @Test
    void patchProfileReturns409WhenTheIfMatchHeaderCarriesAStaleVersionThroughTheFullStack() {
        when(customer360Adapter.fetchProfile("cust-1")).thenReturn(Mono.just(sampleDto()));

        webTestClient.patch().uri("/v1/customers/cust-1/profile")
                .header(HttpHeaders.IF_MATCH, "stale-version")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"phone\":\"0899999999\"}")
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("CF001");
    }

    @Test
    void secondPatchSucceedsWithTheVersionReturnedByAPriorPatchInTheSameVisit() {
        Customer360ProfileDto afterContactSave = new Customer360ProfileDto(
                "0899999999", "123 Moo 4", "Soi 5", "Bang Rak", "Bang Rak", "Bangkok", "10500");
        // First two fetches (the GET, then the first PATCH's version check) see the
        // original state; once the first PATCH "writes", Customer360 reflects it.
        when(customer360Adapter.fetchProfile("cust-1"))
                .thenReturn(Mono.just(sampleDto()), Mono.just(sampleDto()), Mono.just(afterContactSave));
        when(customer360Adapter.updateProfile(eq("cust-1"), any())).thenReturn(Mono.just(afterContactSave));
        String originalVersion = readProfile("cust-1").version();

        String versionAfterContactSave = webTestClient.patch().uri("/v1/customers/cust-1/profile")
                .header(HttpHeaders.IF_MATCH, originalVersion)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"phone\":\"0899999999\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProfileResponse.class)
                .returnResult()
                .getResponseBody()
                .version();

        Customer360ProfileDto afterAddressSave = new Customer360ProfileDto(
                "0899999999", "999 Moo 9", "Soi 5", "Bang Rak", "Bang Rak", "Bangkok", "10500");
        when(customer360Adapter.updateProfile(eq("cust-1"), any())).thenReturn(Mono.just(afterAddressSave));

        webTestClient.patch().uri("/v1/customers/cust-1/profile")
                .header(HttpHeaders.IF_MATCH, versionAfterContactSave)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"addressLine1\":\"999 Moo 9\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.addressLine1").isEqualTo("999 Moo 9");
    }

    @Test
    void secondPatchIsRejectedWhenItReusesTheVersionFromBeforeTheFirstPatchInTheSameVisit() {
        Customer360ProfileDto afterContactSave = new Customer360ProfileDto(
                "0899999999", "123 Moo 4", "Soi 5", "Bang Rak", "Bang Rak", "Bangkok", "10500");
        // First two fetches (the GET, then the first PATCH's version check) see the
        // original state; once the first PATCH "writes", Customer360 reflects it -
        // so replaying the original version on the second PATCH is now stale.
        when(customer360Adapter.fetchProfile("cust-1"))
                .thenReturn(Mono.just(sampleDto()), Mono.just(sampleDto()), Mono.just(afterContactSave));
        when(customer360Adapter.updateProfile(eq("cust-1"), any())).thenReturn(Mono.just(afterContactSave));
        String originalVersion = readProfile("cust-1").version();

        webTestClient.patch().uri("/v1/customers/cust-1/profile")
                .header(HttpHeaders.IF_MATCH, originalVersion)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"phone\":\"0899999999\"}")
                .exchange()
                .expectStatus().isOk();

        webTestClient.patch().uri("/v1/customers/cust-1/profile")
                .header(HttpHeaders.IF_MATCH, originalVersion)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"addressLine1\":\"999 Moo 9\"}")
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void phoneOnlyPatchAppendsAnAuditRecordNamingOnlyPhoneThroughTheFullStack() {
        when(customer360Adapter.fetchProfile("cust-1")).thenReturn(Mono.just(sampleDto()));
        Customer360ProfileDto updatedDto = new Customer360ProfileDto(
                "0899999999", "123 Moo 4", "Soi 5", "Bang Rak", "Bang Rak", "Bangkok", "10500");
        when(customer360Adapter.updateProfile(eq("cust-1"), any())).thenReturn(Mono.just(updatedDto));
        String currentVersion = readProfile("cust-1").version();

        webTestClient.patch().uri("/v1/customers/cust-1/profile")
                .header(HttpHeaders.IF_MATCH, currentVersion)
                .header("X-Actor-Id", "agent-42")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"phone\":\"0899999999\"}")
                .exchange()
                .expectStatus().isOk();

        ArgumentCaptor<Customer360AuditRecordDto> captor = ArgumentCaptor.forClass(Customer360AuditRecordDto.class);
        verify(customer360Adapter).appendAuditRecord(captor.capture());
        Customer360AuditRecordDto record = captor.getValue();
        assertThat(record.customerId()).isEqualTo("cust-1");
        assertThat(record.changedFields()).containsExactly("phone");
        assertThat(record.actor()).isEqualTo("agent-42");
        assertThat(record.timestamp()).isNotNull();
    }

    @Test
    void auditWriteFailureAfterASuccessfulProfileWriteSurfacesAsA502InsteadOfBeingSilentThroughTheFullStack() {
        when(customer360Adapter.fetchProfile("cust-1")).thenReturn(Mono.just(sampleDto()));
        Customer360ProfileDto updatedDto = new Customer360ProfileDto(
                "0899999999", "123 Moo 4", "Soi 5", "Bang Rak", "Bang Rak", "Bangkok", "10500");
        when(customer360Adapter.updateProfile(eq("cust-1"), any())).thenReturn(Mono.just(updatedDto));
        when(customer360Adapter.appendAuditRecord(any())).thenReturn(Mono.error(new AuditWriteFailedException(
                "cust-1", List.of("phone"), "agent-42", new RuntimeException("Customer360 audit endpoint down"))));
        String currentVersion = readProfile("cust-1").version();

        // The profile write above already reached Customer360 and succeeded (updateProfile
        // was stubbed to return updatedDto) before the audit write fails - so a 200 here
        // would mean the discrepancy (persisted change, no audit trail) went undetected.
        webTestClient.patch().uri("/v1/customers/cust-1/profile")
                .header(HttpHeaders.IF_MATCH, currentVersion)
                .header("X-Actor-Id", "agent-42")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"phone\":\"0899999999\"}")
                .exchange()
                .expectStatus().isEqualTo(502)
                .expectBody()
                .jsonPath("$.code").isEqualTo("SF011");
    }
}
