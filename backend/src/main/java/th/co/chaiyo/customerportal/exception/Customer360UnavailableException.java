package th.co.chaiyo.customerportal.exception;

public class Customer360UnavailableException extends PortalException {

    public Customer360UnavailableException(String customerId, Throwable cause) {
        super("SF010", "Customer360 unavailable", "Failed to fetch profile for customer id " + customerId);
        initCause(cause);
    }
}
