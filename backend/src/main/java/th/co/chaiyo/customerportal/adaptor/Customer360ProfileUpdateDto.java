package th.co.chaiyo.customerportal.adaptor;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Wire shape sent to Customer360's profile write endpoint. Null fields are
 * omitted from the outgoing JSON (A-1) so Customer360 performs the partial
 * merge itself instead of us round-tripping a read-modify-write.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Customer360ProfileUpdateDto(
        String phone,
        String addressLine1,
        String addressLine2,
        String subDistrict,
        String district,
        String province,
        String postalCode) {
}
