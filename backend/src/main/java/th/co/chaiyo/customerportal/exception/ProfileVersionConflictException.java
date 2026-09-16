package th.co.chaiyo.customerportal.exception;

public class ProfileVersionConflictException extends PortalException {

    public ProfileVersionConflictException(String customerId) {
        super("CF001", "Profile version conflict",
                "If-Match value does not match the current profile version for customer id " + customerId);
    }
}
