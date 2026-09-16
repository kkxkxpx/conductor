package th.co.chaiyo.customerportal.exception;

public class CustomerNotFoundException extends PortalException {

    public CustomerNotFoundException(String customerId) {
        super("NF001", "Customer not found", "No profile exists for customer id " + customerId);
    }
}
