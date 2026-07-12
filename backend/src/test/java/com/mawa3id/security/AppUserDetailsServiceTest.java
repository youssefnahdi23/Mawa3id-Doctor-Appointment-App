package com.mawa3id.security;

import com.mawa3id.user.Role;
import com.mawa3id.user.User;
import com.mawa3id.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AppUserDetailsService}. The "user not found" branch is
 * otherwise unexercised: it is the path taken when a JWT is validly signed but the
 * user no longer exists.
 */
@ExtendWith(MockitoExtension.class)
class AppUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AppUserDetailsService service;

    @Test
    void loadsExistingUserByEmailWithRoleAuthority() {
        User user = new User("alice@example.com", "hash", Role.PATIENT);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("alice@example.com");

        assertThat(details.getUsername()).isEqualTo("alice@example.com");
        assertThat(details.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_PATIENT");
    }

    @Test
    void throwsWhenUserMissing() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("ghost@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
