package th.co.chaiyo.customerportal.adaptor;

/**
 * Wire shape returned by Customer360's profile endpoint.
 * Field names are assumed 1:1 with our contract (A-1) pending the real Customer360 API spec.
 */
public record Customer360ProfileDto(
        String phone,
        String addressLine1,
        String addressLine2,
        String subDistrict,
        String district,
        String province,
        String postalCode) {
}
