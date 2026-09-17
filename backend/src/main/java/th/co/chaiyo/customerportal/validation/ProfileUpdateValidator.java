package th.co.chaiyo.customerportal.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import th.co.chaiyo.customerportal.model.request.ProfileUpdateRequest;
import th.co.chaiyo.customerportal.model.response.FieldValidationError;

/**
 * R-3 / Q-6: the one rule set for fields with a settled format, applied only
 * when the field is present (R-1: a null field means "leave unchanged" and
 * is never validated). Fields without a settled rule (address lines,
 * sub-district, district, province) are intentionally left unchecked here
 * rather than guessing at a rule that Q-6 hasn't fixed yet.
 */
@Component
public class ProfileUpdateValidator {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{10}$");
    private static final Pattern POSTAL_CODE_PATTERN = Pattern.compile("^[0-9]{5}$");

    public List<FieldValidationError> validate(ProfileUpdateRequest request) {
        List<FieldValidationError> errors = new ArrayList<>();

        if (request.phone() != null && !PHONE_PATTERN.matcher(request.phone()).matches()) {
            errors.add(new FieldValidationError("phone", "Phone number must be exactly 10 digits."));
        }

        if (request.postalCode() != null && !POSTAL_CODE_PATTERN.matcher(request.postalCode()).matches()) {
            errors.add(new FieldValidationError("postalCode", "Postal code must be exactly 5 digits."));
        }

        return errors;
    }
}
