package th.co.chaiyo.customerportal.exception;

import java.util.List;

/**
 * R-9: raised when the profile write to Customer360 already succeeded but
 * the follow-up audit-record write failed. No transaction spans the two
 * remote calls, so this exception is how the discrepancy is surfaced
 * (502 response + ERROR log via GlobalExceptionHandler) instead of being
 * left silently in place.
 */
public class AuditWriteFailedException extends PortalException {

    public AuditWriteFailedException(String customerId, List<String> changedFields, String actor, Throwable cause) {
        super("SF011", "Audit write failed",
                "Failed to record audit entry for customer id " + customerId
                        + " (changedFields=" + changedFields + ", actor=" + actor
                        + "); the profile change was already persisted and requires reconciliation");
        initCause(cause);
    }
}
