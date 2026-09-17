package th.co.chaiyo.customerportal.model.response;

/**
 * One offending field from a 422, matching the FE/BE contract
 * {@code {field, message}}. {@code field} names the request field exactly as
 * sent; {@code message} is English text fit to render directly to a customer.
 */
public record FieldValidationError(String field, String message) {
}
