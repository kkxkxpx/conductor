package th.co.chaiyo.customerportal.exception;

/**
 * Standard error body shape: {@code {code, message, description}}.
 */
public record ApiError(String code, String message, String description) {
}
