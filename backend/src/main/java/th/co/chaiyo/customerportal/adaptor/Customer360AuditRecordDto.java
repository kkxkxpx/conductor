package th.co.chaiyo.customerportal.adaptor;

import java.time.Instant;
import java.util.List;

/**
 * R-8: {customerId, changedFields, actor, timestamp} appended to Customer360
 * for every accepted profile write. Q-3: Customer360 is the chosen system of
 * record for audit entries, so this is both the domain record and the wire
 * shape sent over HTTP. `actor` is whatever the request presented (Q-1 auth
 * is unresolved) — not an authenticated identity.
 */
public record Customer360AuditRecordDto(
        String customerId,
        List<String> changedFields,
        String actor,
        Instant timestamp) {
}
