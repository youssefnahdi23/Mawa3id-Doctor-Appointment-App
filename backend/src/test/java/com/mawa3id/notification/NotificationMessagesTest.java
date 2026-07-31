package com.mawa3id.notification;

import com.mawa3id.user.Role;
import com.mawa3id.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.time.LocalDateTime;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMessagesTest {

    private final NotificationMessages messages = new NotificationMessages(messageSource(), "en");

    private static ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }

    private static User userWithLanguage(String language) {
        User user = new User("sami", "s@x.y", "hash", Role.PATIENT);
        user.setPreferredLanguage(language);
        return user;
    }

    @Test
    void localeForFallsBackToDefaultWhenUnset() {
        assertThat(messages.localeFor(userWithLanguage(null)).getLanguage()).isEqualTo("en");
        assertThat(messages.localeFor(null).getLanguage()).isEqualTo("en");
        assertThat(messages.localeFor(userWithLanguage("ar")).getLanguage()).isEqualTo("ar");
    }

    @Test
    void rendersTitlePerLanguage() {
        assertThat(messages.title(NotificationType.APPOINTMENT_ACCEPTED, Locale.ENGLISH))
                .isEqualTo("Appointment confirmed");
        assertThat(messages.title(NotificationType.APPOINTMENT_ACCEPTED, Locale.FRENCH))
                .isEqualTo("Rendez-vous confirmé");
        assertThat(messages.title(NotificationType.APPOINTMENT_ACCEPTED, Locale.forLanguageTag("ar")))
                .isEqualTo("تم تأكيد الموعد");
    }

    @Test
    void rendersBodyWithFormattedDateInTargetLanguage() {
        Object[] args = {LocalDateTime.of(2026, 1, 5, 9, 30)};

        assertThat(messages.body("notification.accepted.body", args, Locale.ENGLISH))
                .startsWith("Your appointment at");
        assertThat(messages.body("notification.accepted.body", args, Locale.forLanguageTag("ar")))
                .contains("موعدك");
    }
}
