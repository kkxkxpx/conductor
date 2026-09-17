package th.co.chaiyo.customerportal.exception;

import lombok.Getter;
import th.co.chaiyo.customerportal.model.response.ProfileResponse;

/**
 * R-7: carries the current profile (in the GET response shape, with its own
 * version) so the 409 handler can hand it straight back to the caller instead
 * of an error envelope — the caller has nothing else to recover from a
 * conflict without a full reload.
 */
@Getter
public class ProfileVersionConflictException extends PortalException {

    private final ProfileResponse currentProfile;

    public ProfileVersionConflictException(String customerId, ProfileResponse currentProfile) {
        super("CF001", "Profile version conflict",
                "If-Match value does not match the current profile version for customer id " + customerId);
        this.currentProfile = currentProfile;
    }
}
