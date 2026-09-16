package th.co.chaiyo.customerportal.model.request;

/**
 * R-1: any subset of these seven fields may be present; a null field means
 * "leave unchanged", not "clear the value". Fields outside this record are
 * dropped by Jackson (fail-on-unknown-properties is disabled) before this
 * type is ever built.
 */
public record ProfileUpdateRequest(
        String phone,
        String addressLine1,
        String addressLine2,
        String subDistrict,
        String district,
        String province,
        String postalCode) {
}
