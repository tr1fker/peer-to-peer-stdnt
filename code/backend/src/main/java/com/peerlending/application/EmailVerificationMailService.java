package com.peerlending.application;

import com.peerlending.config.AppProperties;
import com.peerlending.persistence.entity.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailVerificationMailService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationMailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final AppProperties appProperties;

    public EmailVerificationMailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${spring.mail.username:}") String mailUsername,
            AppProperties appProperties
    ) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.fromAddress = mailUsername != null && !mailUsername.isBlank() ? mailUsername : "noreply@peerlending.local";
        this.appProperties = appProperties;
    }

    public void sendVerificationLink(UserEntity user, String verificationUrl) {
        String body = """
                Здравствуйте!

                Подтвердите email для аккаунта PeerLend, перейдя по ссылке (скопируйте в браузер, если кнопка не нажимается):
                %s

                Ссылка действует около %s ч. Если вы не регистрировались, проигнорируйте письмо.
                """
                .formatted(verificationUrl, appProperties.getEmail().getVerificationTtlHours());

        if (mailSender == null) {
            log.warn(
                    "Почта не настроена (задайте MAIL_HOST и т.д.) — ссылка подтверждения для {}:\n{}",
                    user.getEmail(),
                    verificationUrl
            );
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromAddress);
        msg.setTo(user.getEmail());
        msg.setSubject("Подтверждение email — PeerLend");
        msg.setText(body);
        try {
            mailSender.send(msg);
            log.info("Письмо подтверждения отправлено на {}", user.getEmail());
        } catch (Exception ex) {
            log.error(
                    "Не удалось отправить письмо на {} (проверьте SPRING_MAIL_* / пароль приложения Gmail). Ссылка для ручного открытия:\n{}",
                    user.getEmail(),
                    verificationUrl,
                    ex
            );
        }
    }
}
