package th.co.chaiyo.customerportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CustomerPortalServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerPortalServiceApplication.class, args);
    }
}
