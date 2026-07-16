package com.mawa3id.auth.challenge;

import com.mawa3id.auth.AuthProperties;
import com.mawa3id.common.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoggingChallengeSenderTest {

    private LoggingChallengeSender sender(AuthProperties.Delivery.Mode mode) {
        AuthProperties properties = new AuthProperties();
        properties.getDelivery().setMode(mode);
        return new LoggingChallengeSender(properties);
    }

    @Test
    void logModeDeliversWithoutThrowing() {
        assertThatCode(() -> sender(AuthProperties.Delivery.Mode.LOG)
                .send(ChallengeChannel.EMAIL, "a@b.co", ChallengePurpose.VERIFY_EMAIL, "123456"))
                .doesNotThrowAnyException();
    }

    @Test
    void disabledModeRefusesWith503() {
        assertThatThrownBy(() -> sender(AuthProperties.Delivery.Mode.DISABLED)
                .send(ChallengeChannel.PHONE, "+2126000000", ChallengePurpose.VERIFY_PHONE, "123456"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus())
                        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }
}
