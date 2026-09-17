package th.co.chaiyo.customerportal.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import th.co.chaiyo.customerportal.adaptor.Customer360Adapter;
import th.co.chaiyo.customerportal.adaptor.Customer360AuditRecordDto;
import th.co.chaiyo.customerportal.adaptor.Customer360ProfileDto;
import th.co.chaiyo.customerportal.adaptor.Customer360ProfileUpdateDto;
import th.co.chaiyo.customerportal.exception.ProfileValidationException;
import th.co.chaiyo.customerportal.exception.ProfileVersionConflictException;
import th.co.chaiyo.customerportal.metrics.ProfileWriteLatencyMetrics;
import th.co.chaiyo.customerportal.model.request.ProfileUpdateRequest;
import th.co.chaiyo.customerportal.model.response.FieldValidationError;
import th.co.chaiyo.customerportal.model.response.ProfileResponse;
import th.co.chaiyo.customerportal.service.CustomerProfileService;
import th.co.chaiyo.customerportal.validation.ProfileUpdateValidator;

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
    private final ProfileUpdateValidator profileUpdateValidator;
    private final Clock clock;
    private final ProfileWriteLatencyMetrics profileWriteLatencyMetrics;

    @Override
    public Mono<ProfileResponse> getProfile(String customerId) {
        return customer360Adapter.fetchProfile(customerId)
                .map(this::toProfileResponse);
    }

    @Override
    public Mono<ProfileResponse> updateProfile(
            String customerId, String ifMatch, ProfileUpdateRequest request, String actor) {
        List<FieldValidationError> validationErrors = profileUpdateValidator.validate(request);
        if (!validationErrors.isEmpty()) {
            return Mono.error(new ProfileValidationException(validationErrors));
        }
        List<String> changedFields = changedFieldNames(request);
        return customer360Adapter.fetchProfile(customerId)
                .flatMap(current -> {
                    if (!computeVersion(current).equals(ifMatch)) {
                        return Mono.error(new ProfileVersionConflictException(customerId));
                    }
                    return profileWriteLatencyMetrics.timeCustomer360Write(
                                    customer360Adapter.updateProfile(customerId, toUpdateDto(request)))
                            .flatMap(updated -> appendAuditRecord(customerId, changedFields, actor)
                                    .thenReturn(updated));
                })
                .map(this::toProfileResponse);
    }

    /**
     * R-9: fired only after the profile write to Customer360 has already
     * succeeded, so a failure here means the profile change is persisted but
     * unaudited. That is surfaced as an error (AuditWriteFailedException,
     * mapped to a 502 by GlobalExceptionHandler) rather than swallowed, since
     * no transaction can span the two remote calls.
     */
    private Mono<Void> appendAuditRecord(String customerId, List<String> changedFields, String actor) {
        Customer360AuditRecordDto record = new Customer360AuditRecordDto(
                customerId, changedFields, actor, clock.instant());
        return customer360Adapter.appendAuditRecord(record);
    }

    /**
     * R-8: only the fields actually present in the request (R-1: null means
     * "leave unchanged") are named as changed.
     */
    private List<String> changedFieldNames(ProfileUpdateRequest request) {
        List<String> names = new ArrayList<>();
        if (request.phone() != null) {
            names.add("phone");
        }
        if (request.addressLine1() != null) {
            names.add("addressLine1");
        }
        if (request.addressLine2() != null) {
            names.add("addressLine2");
        }
        if (request.subDistrict() != null) {
            names.add("subDistrict");
        }
        if (request.district() != null) {
            names.add("district");
        }
        if (request.province() != null) {
            names.add("province");
        }
        if (request.postalCode() != null) {
            names.add("postalCode");
        }
        return List.copyOf(names);
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
