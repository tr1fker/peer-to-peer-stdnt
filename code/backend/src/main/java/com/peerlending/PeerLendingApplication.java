package com.peerlending;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;

@SpringBootApplication(exclude = {MailSenderAutoConfiguration.class})
public class PeerLendingApplication {

    public static void main(String[] args) {
        SpringApplication.run(PeerLendingApplication.class, args);
    }
}
