package th.co.chaiyo.customerportal.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import th.co.chaiyo.customerportal.adaptor.Customer360Adapter;
import th.co.chaiyo.customerportal.adaptor.Customer360ProfileDto;
import th.co.chaiyo.customerportal.adaptor.Customer360ProfileUpdateDto;
import th.co.chaiyo.customerportal.exception.ProfileVersionConflictException;
import th.co.chaiyo.customerportal.model.request.ProfileUpdateRequest;
import th.co.chaiyo.customerportal.model.response.ProfileResponse;
import th.co.chaiyo.customerportal.service.CustomerProfileService;

/**
 * A-2: Customer360's own versioning semantics are not yet known, so this
 * service mints the opaque version itself — a deterministic digest of the
 * profile fields. Two reads of unchanged data therefore yield the same
 * version without requiring any state on our side.
 */
@Service
@RequiredArgsConstructor
public class CustomerProfileServiceImpl implements CustomerProfileService {

    private static final String FIELD_DELIMITER = " ";

    private final Customer360Adapter customer360Adapter;

    @Override
    public Mono<ProfileResponse> getProfile(String customerId) {
        return customer360Adapter.fetchProfile(customerId)
                .map(this::toProfileResponse);
    }

    @Override
    public Mono<ProfileResponse> updateProfile(String customerId, String ifMatch, ProfileUpdateRequest request) {
        return customer360Adapter.fetchProfile(customerId)
                .flatMap(current -> {
                    if (!computeVersion(current).equals(ifMatch)) {
                        return Mono.error(new ProfileVersionConflictException(customerId));
                    }
                    return customer360Adapter.updateProfile(customerId, toUpdateDto(request));
                })
                .map(this::toProfileResponse);
    }

    private Customer360ProfileUpdateDto toUpdateDto(ProfileUpdateRequest request) {
        return new Customer360ProfileUpdateDto(
                request.phone(),
                request.addressLine1(),
                request.addressLine2(),
                request.subDistrict(),
                request.district(),
                request.province(),
                request.postalCode());
    }

    private ProfileResponse toProfileResponse(Customer360ProfileDto profile) {
        String version = computeVersion(profile);
        return new ProfileResponse(
                profile.phone(),
                profile.addressLine1(),
                profile.addressLine2(),
                profile.subDistrict(),
                profile.district(),
                profile.province(),
                profile.postalCode(),
                version);
    }

    private String computeVersion(Customer360ProfileDto profile) {
        String canonical = String.join(FIELD_DELIMITER,
                nullToEmpty(profile.phone()),
                nullToEmpty(profile.addressLine1()),
                nullToEmpty(profile.addressLine2()),
                nullToEmpty(profile.subDistrict()),
                nullToEmpty(profile.district()),
                nullToEmpty(profile.province()),
                nullToEmpty(profile.postalCode()));
        return sha256Hex(canonical);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
