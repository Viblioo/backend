package com.revibe.backend.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.revibe.backend.application.ports.output.UserRepositoryPort;
import com.revibe.backend.domain.exception.UserNotFoundException;
import com.revibe.backend.domain.model.UserStatus;
import com.revibe.backend.support.TestUsers;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetCurrentUserServiceTest {

    @Mock
    private UserRepositoryPort users;

    @Test
    void returnsTheUser() {
        var user = TestUsers.user(UserStatus.ACTIVE);
        when(users.findById(user.getId())).thenReturn(Optional.of(user));

        assertThat(new GetCurrentUserService(users).getCurrentUser(user.getId())).isSameAs(user);
    }

    @Test
    void deletedUserIsNotFound() {
        var user = TestUsers.user(UserStatus.DELETED);
        when(users.findById(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> new GetCurrentUserService(users).getCurrentUser(user.getId()))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void missingUserIsNotFound() {
        var id = UUID.randomUUID();
        when(users.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new GetCurrentUserService(users).getCurrentUser(id))
                .isInstanceOf(UserNotFoundException.class);
    }
}
