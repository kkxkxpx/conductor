package th.co.chaiyo.customerportal.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import th.co.chaiyo.customerportal.adaptor.Customer360Adapter;
import th.co.chaiyo.customerportal.adaptor.Customer360ProfileDto;
import th.co.chaiyo.customerportal.exception.CustomerNotFoundException;
import th.co.chaiyo.customerportal.model.response.ProfileResponse;

@ExtendWith(MockitoExtension.class)
class CustomerProfileServiceImplTest {

    @Mock
    private Customer360Adapter customer360Adapter;

    private CustomerProfileServiceImpl service;

    private Customer360ProfileDto sampleDto() {
        return new Customer360ProfileDto(
                "0812345678",
                "123 Moo 4",
                "Soi 5",
                "Bang Rak",
                "Bang Rak",
                "Bangkok",
                "10500");
    }

    @Test
    void getProfileReturnsAllSevenProfileFieldsFromCustomer360() {
        service = new CustomerProfileServiceImpl(customer360Adapter);
        when(customer360Adapter.fetchProfile("cust-1")).thenReturn(Mono.just(sampleDto()));

        ProfileResponse response = service.getProfile("cust-1").block();

        assertThat(response.phone()).isEqualTo("0812345678");
        assertThat(response.addressLine1()).isEqualTo("123 Moo 4");
        assertThat(response.addressLine2()).isEqualTo("Soi 5");
        assertThat(response.subDistrict()).isEqualTo("Bang Rak");
        assertThat(response.district()).isEqualTo("Bang Rak");
        assertThat(response.province()).isEqualTo("Bangkok");
        assertThat(response.postalCode()).isEqualTo("10500");
    }

    @Test
    void getProfileReturnsNonBlankVersion() {
        service = new CustomerProfileServiceImpl(customer360Adapter);
        when(customer360Adapter.fetchProfile("cust-1")).thenReturn(Mono.just(sampleDto()));

        ProfileResponse response = service.getProfile("cust-1").block();

        assertThat(response.version()).isNotBlank();
    }

    @Test
    void getProfileReturnsSameVersionOnTwoConsecutiveReadsWithNoIntervalWrite() {
        service = new CustomerProfileServiceImpl(customer360Adapter);
        when(customer360Adapter.fetchProfile("cust-1"))
                .thenReturn(Mono.just(sampleDto()), Mono.just(sampleDto()));

        String firstVersion = service.getProfile("cust-1").block().version();
        String secondVersion = service.getProfile("cust-1").block().version();

        assertThat(secondVersion).isEqualTo(firstVersion);
    }

    @Test
    void getProfileReturnsDifferentVersionWhenAProfileFieldDiffers() {
        service = new CustomerProfileServiceImpl(customer360Adapter);
        Customer360ProfileDto changed = new Customer360ProfileDto(
                "0899999999", "123 Moo 4", "Soi 5", "Bang Rak", "Bang Rak", "Bangkok", "10500");
        when(customer360Adapter.fetchProfile("cust-1"))
                .thenReturn(Mono.just(sampleDto()), Mono.just(changed));

        String firstVersion = service.getProfile("cust-1").block().version();
        String secondVersion = service.getProfile("cust-1").block().version();

        assertThat(secondVersion).isNotEqualTo(firstVersion);
    }

    @Test
    void getProfilePropagatesCustomerNotFoundFromAdapter() {
        service = new CustomerProfileServiceImpl(customer360Adapter);
        when(customer360Adapter.fetchProfile("missing"))
                .thenReturn(Mono.error(new CustomerNotFoundException("missing")));

        StepVerifier.create(service.getProfile("missing"))
                .expectError(CustomerNotFoundException.class)
                .verify();
    }
}
