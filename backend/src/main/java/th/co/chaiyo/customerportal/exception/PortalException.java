package th.co.chaiyo.customerportal.exception;

import lombok.Getter;

@Getter
public class PortalException extends RuntimeException {

    private final String code;
    private final String description;

    public PortalException(String code, String message, String description) {
        super(message);
        this.code = code;
        this.description = description;
    }
}
