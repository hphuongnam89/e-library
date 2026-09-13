package vn.edu.phuxuan.elib;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class ElibApplication {
    public static void main(String[] args) {
        SpringApplication.run(ElibApplication.class, args);
    }
}
