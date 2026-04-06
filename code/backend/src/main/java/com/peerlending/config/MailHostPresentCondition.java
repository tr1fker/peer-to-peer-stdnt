package com.peerlending.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/** True only when {@code spring.mail.host} задан и не пустой (из SPRING_MAIL_HOST и т.п.). */
public final class MailHostPresentCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return StringUtils.hasText(context.getEnvironment().getProperty("spring.mail.host"));
    }
}
