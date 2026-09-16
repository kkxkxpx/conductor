package th.co.chaiyo.customerportal.service;

import reactor.core.publisher.Mono;
import th.co.chaiyo.customerportal.model.response.ProfileResponse;

public interface CustomerProfileService {

    Mono<ProfileResponse> getProfile(String customerId);
}
