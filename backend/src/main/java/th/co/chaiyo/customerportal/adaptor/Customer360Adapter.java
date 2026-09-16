package th.co.chaiyo.customerportal.adaptor;

import reactor.core.publisher.Mono;

public interface Customer360Adapter {

    Mono<Customer360ProfileDto> fetchProfile(String customerId);
}
