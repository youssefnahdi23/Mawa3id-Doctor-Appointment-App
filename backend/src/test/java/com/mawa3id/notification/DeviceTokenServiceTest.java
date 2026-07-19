package com.mawa3id.notification;

import com.mawa3id.user.User;
import com.mawa3id.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceTokenServiceTest {

    @Mock private DeviceTokenRepository deviceTokenRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private DeviceTokenService service;

    private static final long USER_ID = 7L;
    private static final long OTHER_USER_ID = 8L;
    private static final String TOKEN = "fcm-token-abc";

    private User userRef(long id) {
        User user = org.mockito.Mockito.mock(User.class);
        lenient().when(user.getId()).thenReturn(id);
        return user;
    }

    @Test
    void registerInsertsNewTokenWithParsedPlatform() {
        User user = userRef(USER_ID);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
        when(deviceTokenRepository.findByToken(TOKEN)).thenReturn(Optional.empty());

        service.register(USER_ID, TOKEN, "android");

        ArgumentCaptor<DeviceToken> captor = ArgumentCaptor.forClass(DeviceToken.class);
        verify(deviceTokenRepository).saveAndFlush(captor.capture());
        DeviceToken saved = captor.getValue();
        assertThat(saved.getToken()).isEqualTo(TOKEN);
        assertThat(saved.getPlatform()).isEqualTo(DevicePlatform.ANDROID);
        assertThat(saved.getRecipient()).isSameAs(user);
    }

    @Test
    void registerUnknownPlatformFallsBackToUnknown() {
        User user = userRef(USER_ID);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
        when(deviceTokenRepository.findByToken(TOKEN)).thenReturn(Optional.empty());

        service.register(USER_ID, TOKEN, "blackberry");

        ArgumentCaptor<DeviceToken> captor = ArgumentCaptor.forClass(DeviceToken.class);
        verify(deviceTokenRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getPlatform()).isEqualTo(DevicePlatform.UNKNOWN);
    }

    @Test
    void registerBlankPlatformIsUnknown() {
        User user = userRef(USER_ID);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
        when(deviceTokenRepository.findByToken(TOKEN)).thenReturn(Optional.empty());

        service.register(USER_ID, TOKEN, "  ");

        ArgumentCaptor<DeviceToken> captor = ArgumentCaptor.forClass(DeviceToken.class);
        verify(deviceTokenRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getPlatform()).isEqualTo(DevicePlatform.UNKNOWN);
    }

    @Test
    void registerExistingSameUserTouchesAndUpdatesPlatform() {
        User user = userRef(USER_ID);
        DeviceToken existing = new DeviceToken(user, TOKEN, DevicePlatform.UNKNOWN);
        when(deviceTokenRepository.findByToken(TOKEN)).thenReturn(Optional.of(existing));

        service.register(USER_ID, TOKEN, "ios");

        assertThat(existing.getPlatform()).isEqualTo(DevicePlatform.IOS);
        assertThat(existing.getLastSeenAt()).isNotNull();
        verify(deviceTokenRepository, never()).saveAndFlush(any());
    }

    @Test
    void registerExistingSameUserBlankPlatformOnlyTouches() {
        User user = userRef(USER_ID);
        DeviceToken existing = new DeviceToken(user, TOKEN, DevicePlatform.ANDROID);
        when(deviceTokenRepository.findByToken(TOKEN)).thenReturn(Optional.of(existing));

        service.register(USER_ID, TOKEN, null);

        // Platform kept; token merely touched.
        assertThat(existing.getPlatform()).isEqualTo(DevicePlatform.ANDROID);
        assertThat(existing.getLastSeenAt()).isNotNull();
    }

    @Test
    void registerExistingDifferentUserReassigns() {
        DeviceToken existing = new DeviceToken(userRef(OTHER_USER_ID), TOKEN, DevicePlatform.ANDROID);
        when(deviceTokenRepository.findByToken(TOKEN)).thenReturn(Optional.of(existing));
        User newOwner = userRef(USER_ID);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(newOwner);

        service.register(USER_ID, TOKEN, "ios");

        assertThat(existing.getRecipient()).isSameAs(newOwner);
        assertThat(existing.getPlatform()).isEqualTo(DevicePlatform.IOS);
    }

    @Test
    void registerRecoversFromConcurrentInsert() {
        User user = userRef(USER_ID);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
        DeviceToken raceWinner = new DeviceToken(userRef(OTHER_USER_ID), TOKEN, DevicePlatform.ANDROID);
        when(deviceTokenRepository.findByToken(TOKEN))
                .thenReturn(Optional.empty())   // initial lookup
                .thenReturn(Optional.of(raceWinner)); // after the constraint violation
        when(deviceTokenRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate token"));

        service.register(USER_ID, TOKEN, "android");

        assertThat(raceWinner.getRecipient()).isSameAs(user);
    }

    @Test
    void unregisterDeletesByToken() {
        service.unregister(TOKEN);
        verify(deviceTokenRepository).deleteByToken(TOKEN);
    }

    @Test
    void tokensForMapsToRawTokens() {
        DeviceToken a = new DeviceToken(userRef(USER_ID), "t1", DevicePlatform.ANDROID);
        DeviceToken b = new DeviceToken(userRef(USER_ID), "t2", DevicePlatform.IOS);
        when(deviceTokenRepository.findByRecipientId(USER_ID)).thenReturn(List.of(a, b));

        assertThat(service.tokensFor(USER_ID)).containsExactly("t1", "t2");
    }
}
