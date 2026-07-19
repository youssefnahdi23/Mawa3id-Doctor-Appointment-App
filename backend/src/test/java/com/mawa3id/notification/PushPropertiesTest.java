package com.mawa3id.notification;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PushPropertiesTest {

    @Test
    void isConfiguredRequiresEnabledAndCredentials() {
        PushProperties props = new PushProperties();
        // Enabled by default, but no credentials yet.
        assertThat(props.isEnabled()).isTrue();
        assertThat(props.isConfigured()).isFalse();

        props.setCredentials("{\"type\":\"service_account\"}");
        assertThat(props.isConfigured()).isTrue();

        props.setEnabled(false);
        assertThat(props.isConfigured()).isFalse();
    }

    @Test
    void exposesBoundValues() {
        PushProperties props = new PushProperties();
        props.setEnabled(true);
        props.setCredentials("creds");
        props.setProjectId("mawa3id-prod");

        assertThat(props.getCredentials()).isEqualTo("creds");
        assertThat(props.getProjectId()).isEqualTo("mawa3id-prod");
    }
}
