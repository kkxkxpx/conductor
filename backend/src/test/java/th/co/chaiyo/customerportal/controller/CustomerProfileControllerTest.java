package th.co.chaiyo.customerportal.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;
import th.co.chaiyo.customerportal.exception.Customer360UnavailableException;
import th.co.chaiyo.customerportal.exception.CustomerNotFoundException;
import th.co.chaiyo.customerportal.exception.ProfileVersionConflictException;
import th.co.chaiyo.customerportal.model.request.ProfileUpdateRequest;
import th.co.chaiyo.customerportal.model.response.ProfileResponse;
import th.co.chaiyo.customerportal.service.CustomerProfileService;

@WebFluxTest(controllers = CustomerProfileController.class)
class CustomerProfileControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CustomerProfileService customerProfileService;

    @Test
    void getProfileReturns200WithAllProfileFieldsAndVersion() {
        ProfileResponse response = new ProfileResponse(
                "0812345678", "123 Moo 4", "Soi 5", "Bang Rak", "Bang Rak", "Bangkok", "10500", "v-abc123");
        when(customerProfileService.getProfile("cust-1")).thenReturn(Mono.just(response));

        webTestClient.get().uri("/v1/customers/cust-1/profile")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.phone").isEqualTo("0812345678")
                .jsonPath("$.addressLine1").isEqualTo("123 Moo 4")
                .jsonPath("$.addressLine2").isEqualTo("Soi 5")
                .jsonPath("$.subDistrict").isEqualTo("Bang Rak")
                .jsonPath("$.district").isEqualTo("Bang Rak")
                .jsonPath("$.province").isEqualTo("Bangkok")
                .jsonPath("$.postalCode").isEqualTo("10500")
                .jsonPath("$.version").isEqualTo("v-abc123");
    }

    @Test
    void getProfileRequestsTheServiceWithThePathCustomerId() {
        ProfileResponse response = new ProfileResponse(
                "0812345678", "123 Moo 4", "Soi 5", "Bang Rak", "Bang Rak", "Bangkok", "10500", "v-abc123");
        when(customerProfileService.getProfile(eq("cust-42"))).thenReturn(Mono.just(response));

        webTestClient.get().uri("/v1/customers/cust-42/profile")
                .exchange()
                .expectStatus().isOk();

        verify(customerProfileService).getProfile("cust-42");
    }

    @Test
    void getProfileReturns404WhenCustomerIsNotFound() {
        when(customerProfileService.getProfile("missing"))
                .thenReturn(Mono.error(new CustomerNotFoundException("missing")));

        webTestClient.get().uri("/v1/customers/missing/profile")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("NF001");
    }

    @Test
    void getProfileReturns502WhenCustomer360IsUnavailable() {
        when(customerProfileService.getProfile("cust-1"))
                .thenReturn(Mono.error(new Customer360UnavailableException("cust-1", new RuntimeException("boom"))));

        webTestClient.get().uri("/v1/customers/cust-1/profile")
                .exchange()
                .expectStatus().isEqualTo(502)
                .expectBody()
                .jsonPath("$.code").isEqualTo("SF010");
    }

    @Test
    void updateProfileReturns200WithThePatchedProfileWhenIfMatchMatches() {
        ProfileResponse response = new ProfileResponse(
                "0899999999", "123 Moo 4", "Soi 5", "Bang Rak", "Bang Rak", "Bangkok", "10500", "v-new456");
        when(customerProfileService.updateProfile(eq("cust-1"), eq("v-abc123"), any(ProfileUpdateRequest.class)))
                .thenReturn(Mono.just(response));

        webTestClient.patch().uri("/v1/customers/cust-1/profile")
                .header("If-Match", "v-abc123")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"phone\":\"0899999999\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.phone").isEqualTo("0899999999")
                .jsonPath("$.version").isEqualTo("v-new456");
    }

    @Test
    void updateProfileForwardsTheIfMatchHeaderAndBodyToTheService() {
        ProfileResponse response = new ProfileResponse(
                "0899999999", "123 Moo 4", "Soi 5", "Bang Rak", "Bang Rak", "Bangkok", "10500", "v-new456");
        when(customerProfileService.updateProfile(eq("cust-1"), eq("v-abc123"), any(ProfileUpdateRequest.class)))
                .thenReturn(Mono.just(response));

        webTestClient.patch().uri("/v1/customers/cust-1/profile")
                .header("If-Match", "v-abc123")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"phone\":\"0899999999\"}")
                .exchange()
                .expectStatus().isOk();

        verify(customerProfileService).updateProfile(
                eq("cust-1"), eq("v-abc123"), eq(new ProfileUpdateRequest(
                        "0899999999", null, null, null, null, null, null)));
    }

    @Test
    void updateProfileIgnoresFieldsOutsideTheSevenNamedProfileFields() {
        ProfileResponse response = new ProfileResponse(
                "0899999999", "123 Moo 4", "Soi 5", "Bang Rak", "Bang Rak", "Bangkok", "10500", "v-new456");
        when(customerProfileService.updateProfile(eq("cust-1"), eq("v-abc123"), any(ProfileUpdateRequest.class)))
                .thenReturn(Mono.just(response));

        webTestClient.patch().uri("/v1/customers/cust-1/profile")
                .header("If-Match", "v-abc123")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"phone\":\"0899999999\",\"isAdmin\":true}")
                .exchange()
                .expectStatus().isOk();

        verify(customerProfileService).updateProfile(
                eq("cust-1"), eq("v-abc123"), eq(new ProfileUpdateRequest(
                        "0899999999", null, null, null, null, null, null)));
    }

    @Test
    void updateProfileReturns409WithTheCurrentProfileWhenTheServiceReportsAVersionConflict() {
        ProfileResponse currentProfile = new ProfileResponse(
                "0812345678", "123 Moo 4", "Soi 5", "Bang Rak", "Bang Rak", "Bangkok", "10500", "v-current789");
        when(customerProfileService.updateProfile(eq("cust-1"), eq("stale-version"), any(ProfileUpdateRequest.class)))
                .thenReturn(Mono.error(new ProfileVersionConflictException("cust-1", currentProfile)));

        webTestClient.patch().uri("/v1/customers/cust-1/profile")
                .header("If-Match", "stale-version")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"phone\":\"0899999999\"}")
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.phone").isEqualTo("0812345678")
                .jsonPath("$.version").isEqualTo("v-current789");
    }

    @Test
    void updateProfileReturns400WhenTheIfMatchHeaderIsMissing() {
        webTestClient.patch().uri("/v1/customers/cust-1/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"phone\":\"0899999999\"}")
                .exchange()
                .expectStatus().isBadRequest();
    }
}
