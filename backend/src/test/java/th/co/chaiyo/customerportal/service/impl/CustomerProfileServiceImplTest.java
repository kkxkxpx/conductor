package th.co.chaiyo.customerportal.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import th.co.chaiyo.customerportal.adaptor.Customer360Adapter;
import th.co.chaiyo.customerportal.adaptor.Customer360AuditRecordDto;
import th.co.chaiyo.customerportal.adaptor.Customer360ProfileDto;
import th.co.chaiyo.customerportal.adaptor.Customer360ProfileUpdateDto;
import th.co.chaiyo.customerportal.exception.AuditWriteFailedException;
import th.co.chaiyo.customerportal.exception.CustomerNotFoundException;
import th.co.chaiyo.customerportal.exception.ProfileVersionConflictException;
import th.co.chaiyo.customerportal.model.request.ProfileUpdateRequest;
import th.co.chaiyo.customerportal.model.response.ProfileResponse;
import th.co.chaiyo.customerportal.validation.ProfileUpdateValidator;

@ExtendWith(MockitoExtension.class)
class CustomerProfileServiceImplTest {

    @Mock
    private Customer360Adapter customer360Adapter;

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-17T10:00:00Z"), ZoneOffset.UTC);

    private CustomerProfileServiceImpl service;

    private CustomerProfileServiceImpl newService() {
        lenient().when(customer360Adapter.appendAuditRecord(any())).thenReturn(Mono.empty());
        return new CustomerProfileServiceImpl(customer360Adapter, new ProfileUpdateValidator(), clock);
    }

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
        service = newService();
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
        service = newService();
        when(customer360Adapter.fetchProfile("cust-1")).thenReturn(Mono.just(sampleDto()));

        ProfileResponse response = service.getProfile("cust-1").block();

        assertThat(response.version()).isNotBlank();
    }

    @Test
    void getProfileReturnsSameVersionOnTwoConsecutiveReadsWithNoIntervalWrite() {
        service = newService();
        when(customer360Adapter.fetchProfile("cust-1"))
                .thenReturn(Mono.just(sampleDto()), Mono.just(sampleDto()));

        String firstVersion = service.getProfile("cust-1").block().version();
        String secondVersion = service.getProfile("cust-1").block().version();

        assertThat(secondVersion).isEqualTo(firstVersion);
    }

    @Test
    void getProfileReturnsDifferentVersionWhenAProfileFieldDiffers() {
        service = newService();
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
        service = newService();
        when(customer360Adapter.fetchProfile("missing"))
                .thenReturn(Mono.error(new CustomerNotFoundException("missing")));

        StepVerifier.create(service.getProfile("missing"))
                .expectError(CustomerNotFoundException.class)
                .verify();
    }

    @Test
    void updateProfileSendsOnlyThePhoneFieldWhenOnlyPhoneIsProvided() {
        service = newService();
        when(customer360Adapter.fetchProfile("cust-1")).thenReturn(Mono.just(sampleDto()));
        String currentVersion = service.getProfile("cust-1").block().version();
        when(customer360Adapter.updateProfile(eq("cust-1"), any())).thenReturn(Mono.just(sampleDto()));
        ProfileUpdateRequest request = new ProfileUpdateRequest("0899999999", null, null, null, null, null, null);

        service.updateProfile("cust-1", currentVersion, request, "agent-1").block();

        ArgumentCaptor<Customer360ProfileUpdateDto> captor = ArgumentCaptor.forClass(Customer360ProfileUpdateDto.class);
        verify(customer360Adapter).updateProfile(eq("cust-1"), captor.capture());
        assertThat(captor.getValue().phone()).isEqualTo("0899999999");
        assertThat(captor.getValue().addressLine1()).isNull();
        assertThat(captor.getValue().addressLine2()).isNull();
        assertThat(captor.getValue().subDistrict()).isNull();
        assertThat(captor.getValue().district()).isNull();
        assertThat(captor.getValue().province()).isNull();
        assertThat(captor.getValue().postalCode()).isNull();
    }

    @Test
    void updateProfileRejectsWithVersionConflictWhenIfMatchDoesNotMatchTheCurrentVersion() {
        service = newService();
        when(customer360Adapter.fetchProfile("cust-1")).thenReturn(Mono.just(sampleDto()));
        ProfileUpdateRequest request = new ProfileUpdateRequest("0899999999", null, null, null, null, null, null);

        StepVerifier.create(service.updateProfile("cust-1", "stale-version", request, "agent-1"))
                .expectError(ProfileVersionConflictException.class)
                .verify();
    }

    @Test
    void updateProfileNeverWritesToCustomer360WhenIfMatchDoesNotMatch() {
        service = newService();
        when(customer360Adapter.fetchProfile("cust-1")).thenReturn(Mono.just(sampleDto()));
        ProfileUpdateRequest request = new ProfileUpdateRequest("0899999999", null, null, null, null, null, null);

        StepVerifier.create(service.updateProfile("cust-1", "stale-version", request, "agent-1"))
                .expectError(ProfileVersionConflictException.class)
                .verify();

        verify(customer360Adapter, never()).updateProfile(any(), any());
    }

    @Test
    void updateProfileReturnsAllSevenFieldsAtTheirNewValuesOnSuccess() {
        service = newService();
        when(customer360Adapter.fetchProfile("cust-1")).thenReturn(Mono.just(sampleDto()));
        String currentVersion = service.getProfile("cust-1").block().version();
        Customer360ProfileDto updatedDto = new Customer360ProfileDto(
                "0899999999", "999 Moo 9", "Soi 9", "Silom", "Bang Rak", "Bangkok", "10500");
        when(customer360Adapter.updateProfile(eq("cust-1"), any())).thenReturn(Mono.just(updatedDto));
        ProfileUpdateRequest request = new ProfileUpdateRequest(
                "0899999999", "999 Moo 9", "Soi 9", "Silom", null, null, null);

        ProfileResponse response = service.updateProfile("cust-1", currentVersion, request, "agent-1").block();

        assertThat(response.phone()).isEqualTo("0899999999");
        assertThat(response.addressLine1()).isEqualTo("999 Moo 9");
        assertThat(response.addressLine2()).isEqualTo("Soi 9");
        assertThat(response.subDistrict()).isEqualTo("Silom");
        assertThat(response.district()).isEqualTo("Bang Rak");
        assertThat(response.province()).isEqualTo("Bangkok");
        assertThat(response.postalCode()).isEqualTo("10500");
    }

    @Test
    void updateProfileAppendsAnAuditRecordNamingOnlyPhoneWhenOnlyPhoneIsProvided() {
        service = newService();
        when(customer360Adapter.fetchProfile("cust-1")).thenReturn(Mono.just(sampleDto()));
        String currentVersion = service.getProfile("cust-1").block().version();
        when(customer360Adapter.updateProfile(eq("cust-1"), any())).thenReturn(Mono.just(sampleDto()));
        ProfileUpdateRequest request = new ProfileUpdateRequest("0899999999", null, null, null, null, null, null);

        service.updateProfile("cust-1", currentVersion, request, "agent-1").block();

        ArgumentCaptor<Customer360AuditRecordDto> captor = ArgumentCaptor.forClass(Customer360AuditRecordDto.class);
        verify(customer360Adapter).appendAuditRecord(captor.capture());
        assertThat(captor.getValue().customerId()).isEqualTo("cust-1");
        assertThat(captor.getValue().changedFields()).containsExactly("phone");
        assertThat(captor.getValue().actor()).isEqualTo("agent-1");
        assertThat(captor.getValue().timestamp()).isEqualTo(clock.instant());
    }

    @Test
    void updateProfileSurfacesAnErrorWhenTheAuditWriteFailsAfterTheProfileWriteSucceeded() {
        service = new CustomerProfileServiceImpl(customer360Adapter, new ProfileUpdateValidator(), clock);
        when(customer360Adapter.fetchProfile("cust-1")).thenReturn(Mono.just(sampleDto()));
        String currentVersion = service.getProfile("cust-1").block().version();
        when(customer360Adapter.updateProfile(eq("cust-1"), any())).thenReturn(Mono.just(sampleDto()));
        when(customer360Adapter.appendAuditRecord(any()))
                .thenReturn(Mono.error(new AuditWriteFailedException(
                        "cust-1", java.util.List.of("phone"), "agent-1", new RuntimeException("boom"))));
        ProfileUpdateRequest request = new ProfileUpdateRequest("0899999999", null, null, null, null, null, null);

        StepVerifier.create(service.updateProfile("cust-1", currentVersion, request, "agent-1"))
                .expectError(AuditWriteFailedException.class)
                .verify();
    }

    @Test
    void updateProfileReturnsANewVersionDifferentFromTheVersionItWasCalledWith() {
        service = newService();
        when(customer360Adapter.fetchProfile("cust-1")).thenReturn(Mono.just(sampleDto()));
        String currentVersion = service.getProfile("cust-1").block().version();
        Customer360ProfileDto updatedDto = new Customer360ProfileDto(
                "0899999999", "123 Moo 4", "Soi 5", "Bang Rak", "Bang Rak", "Bangkok", "10500");
        when(customer360Adapter.updateProfile(eq("cust-1"), any())).thenReturn(Mono.just(updatedDto));
        ProfileUpdateRequest request = new ProfileUpdateRequest("0899999999", null, null, null, null, null, null);

        ProfileResponse response = service.updateProfile("cust-1", currentVersion, request, "agent-1").block();

        assertThat(response.version()).isNotEqualTo(currentVersion);
    }
}
