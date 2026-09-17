package th.co.chaiyo.customerportal.adaptor.impl;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import th.co.chaiyo.customerportal.adaptor.Customer360Adapter;
import th.co.chaiyo.customerportal.adaptor.Customer360AuditRecordDto;
import th.co.chaiyo.customerportal.adaptor.Customer360ProfileDto;
import th.co.chaiyo.customerportal.adaptor.Customer360ProfileUpdateDto;
import th.co.chaiyo.customerportal.exception.AuditWriteFailedException;
import th.co.chaiyo.customerportal.exception.Customer360UnavailableException;
import th.co.chaiyo.customerportal.exception.CustomerNotFoundException;

@Slf4j
@Component
@RequiredArgsConstructor
public class Customer360AdapterImpl implements Customer360Adapter {

    private final WebClient customer360WebClient;

    @Override
    public Mono<Customer360ProfileDto> fetchProfile(String customerId) {
        return customer360WebClient.get()
                .uri("/customers/{id}/profile", customerId)
                .retrieve()
                .bodyToMono(Customer360ProfileDto.class)
                .onErrorMap(WebClientResponseException.class, ex -> mapError(customerId, ex))
                .onErrorMap(ex -> !(ex instanceof CustomerNotFoundException)
                                && !(ex instanceof Customer360UnavailableException),
                        ex -> new Customer360UnavailableException(customerId, ex));
    }

    @Override
    public Mono<Customer360ProfileDto> updateProfile(String customerId, Customer360ProfileUpdateDto update) {
        return customer360WebClient.patch()
                .uri("/customers/{id}/profile", customerId)
                .bodyValue(update)
                .retrieve()
                .bodyToMono(Customer360ProfileDto.class)
                .onErrorMap(WebClientResponseException.class, ex -> mapError(customerId, ex))
                .onErrorMap(ex -> !(ex instanceof CustomerNotFoundException)
                                && !(ex instanceof Customer360UnavailableException),
                        ex -> new Customer360UnavailableException(customerId, ex));
    }

    @Override
    public Mono<Void> appendAuditRecord(Customer360AuditRecordDto record) {
        return customer360WebClient.post()
                .uri("/customers/{id}/audit-entries", record.customerId())
                .bodyValue(record)
                .retrieve()
                .toBodilessEntity()
                .then()
                .onErrorMap(ex -> !(ex instanceof AuditWriteFailedException),
                        ex -> new AuditWriteFailedException(
                                record.customerId(), record.changedFields(), record.actor(), ex));
    }

    private RuntimeException mapError(String customerId, WebClientResponseException ex) {
        if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
            return new CustomerNotFoundException(customerId);
        }
        return new Customer360UnavailableException(customerId, ex);
    }
}
