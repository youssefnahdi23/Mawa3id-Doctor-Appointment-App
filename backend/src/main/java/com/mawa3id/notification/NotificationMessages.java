package com.mawa3id.notification;

import com.mawa3id.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Renders notification titles and bodies in a recipient's language via the
 * {@link MessageSource} bundles. Title keys are derived from the notification type
 * ({@code notification.<type>.title}); body keys are supplied by the caller. Any
 * {@link LocalDateTime} argument is pre-formatted in the target locale before interpolation.
 */
@Component
public class NotificationMessages {

    private final MessageSource messageSource;
    private final String defaultLanguage;

    public NotificationMessages(MessageSource messageSource,
                                @Value("${mawa3id.i18n.default-language:en}") String defaultLanguage) {
        this.messageSource = messageSource;
        this.defaultLanguage = defaultLanguage;
    }

    public Locale localeFor(User user) {
        String lang = user != null ? user.getPreferredLanguage() : null;
        if (!StringUtils.hasText(lang)) {
            lang = defaultLanguage;
        }
        return Locale.forLanguageTag(lang);
    }

    public String title(NotificationType type, Locale locale) {
        return messageSource.getMessage(titleKey(type), null, locale);
    }

    public String body(String bodyKey, Object[] args, Locale locale) {
        return messageSource.getMessage(bodyKey, formatArgs(args, locale), locale);
    }

    private static String titleKey(NotificationType type) {
        return "notification." + type.name().toLowerCase(Locale.ROOT) + ".title";
    }

    private static Object[] formatArgs(Object[] args, Locale locale) {
        if (args == null) {
            return null;
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", locale);
        Object[] out = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            out[i] = args[i] instanceof LocalDateTime dt ? dt.format(fmt) : args[i];
        }
        return out;
    }
}
