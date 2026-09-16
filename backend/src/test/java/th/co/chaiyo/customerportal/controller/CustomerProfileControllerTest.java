package th.co.chaiyo.customerportal.controller;

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
}
