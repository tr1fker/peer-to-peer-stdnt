package com.peerlending.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Свой {@link JavaMailSender} с явным STARTTLS — иначе Gmail (порт 587) отвечает 530.
 * Автоконфиг MailSender отключён в {@link com.peerlending.PeerLendingApplication}.
 */
@Configuration
@Conditional(MailHostPresentCondition.class)
public class MailSenderConfiguration {

    @Bean
    public JavaMailSender javaMailSender(Environment env) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(env.getRequiredProperty("spring.mail.host").trim());

        String portRaw = env.getProperty("spring.mail.port", "587");
        sender.setPort(Integer.parseInt(portRaw.trim()));

        sender.setUsername(env.getProperty("spring.mail.username", ""));
        sender.setPassword(env.getProperty("spring.mail.password", ""));

        Properties p = new Properties();
        p.put("mail.transport.protocol", "smtp");
        p.put("mail.smtp.auth", "true");
        p.put("mail.smtp.starttls.enable", "true");
        p.put("mail.smtp.starttls.required", "true");
        sender.setJavaMailProperties(p);
        return sender;
    }
}
