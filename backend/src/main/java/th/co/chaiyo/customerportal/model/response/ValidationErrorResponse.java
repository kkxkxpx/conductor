package th.co.chaiyo.customerportal.model.response;

import java.util.List;

/**
 * 422 response body: {@code { errors: [ {field, message} ] } }, one entry
 * per offending field.
 */
public record ValidationErrorResponse(List<FieldValidationError> errors) {
}
