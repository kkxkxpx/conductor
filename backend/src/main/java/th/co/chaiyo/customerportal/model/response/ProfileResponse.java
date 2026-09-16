package th.co.chaiyo.customerportal.model.response;

/**
 * R-5: {@code version} is an opaque string the client sends back verbatim.
 * It is a response body field (not an ETag header) so a cross-origin SPA
 * can read it without extra header exposure.
 */
public record ProfileResponse(
        String phone,
        String addressLine1,
        String addressLine2,
        String subDistrict,
        String district,
        String province,
        String postalCode,
        String version) {
}
