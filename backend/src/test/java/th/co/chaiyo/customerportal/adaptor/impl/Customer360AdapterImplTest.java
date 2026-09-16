package th.co.chaiyo.customerportal.adaptor.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import th.co.chaiyo.customerportal.adaptor.Customer360ProfileDto;
import th.co.chaiyo.customerportal.exception.Customer360UnavailableException;
import th.co.chaiyo.customerportal.exception.CustomerNotFoundException;

class Customer360AdapterImplTest {

    private Customer360AdapterImpl adapterRespondingWith(ClientResponse response) {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> Mono.just(response))
                .build();
        return new Customer360AdapterImpl(webClient);
    }

    @Test
    void fetchProfileReturnsTheDtoOnASuccessfulResponse() {
        String body = """
                {"phone":"0812345678","addressLine1":"123 Moo 4","addressLine2":"Soi 5",
                "subDistrict":"Bang Rak","district":"Bang Rak","province":"Bangkok","postalCode":"10500"}
                """;
        ClientResponse response = ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build();
        Customer360AdapterImpl adapter = adapterRespondingWith(response);

        Customer360ProfileDto dto = adapter.fetchProfile("cust-1").block();

        assertThat(dto.phone()).isEqualTo("0812345678");
        assertThat(dto.postalCode()).isEqualTo("10500");
    }

    @Test
    void fetchProfileMapsA404ResponseToCustomerNotFoundException() {
        ClientResponse response = ClientResponse.create(HttpStatus.NOT_FOUND)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body("{}")
                .build();
        Customer360AdapterImpl adapter = adapterRespondingWith(response);

        StepVerifier.create(adapter.fetchProfile("missing"))
                .expectError(CustomerNotFoundException.class)
                .verify();
    }

    @Test
    void fetchProfileMapsA500ResponseToCustomer360UnavailableException() {
        ClientResponse response = ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body("{}")
                .build();
        Customer360AdapterImpl adapter = adapterRespondingWith(response);

        StepVerifier.create(adapter.fetchProfile("cust-1"))
                .expectError(Customer360UnavailableException.class)
                .verify();
    }
}
