package th.co.chaiyo.customerportal.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import th.co.chaiyo.customerportal.model.response.ProfileResponse;
import th.co.chaiyo.customerportal.service.CustomerProfileService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/customers")
public class CustomerProfileController {

    private final CustomerProfileService customerProfileService;

    @GetMapping("/{id}/profile")
    public Mono<ResponseEntity<ProfileResponse>> getProfile(@PathVariable("id") String id) {
        return customerProfileService.getProfile(id)
                .map(ResponseEntity::ok);
    }
}
