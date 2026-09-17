package th.co.chaiyo.customerportal.exception;

import java.util.List;

import lombok.Getter;
import th.co.chaiyo.customerportal.model.response.FieldValidationError;

/**
 * R-3: raised before any upstream call is made, so a validation failure
 * never results in a partial or full write.
 */
@Getter
public class ProfileValidationException extends RuntimeException {

    private final List<FieldValidationError> errors;

    public ProfileValidationException(List<FieldValidationError> errors) {
        super("Profile update failed field validation");
        this.errors = errors;
    }
}
