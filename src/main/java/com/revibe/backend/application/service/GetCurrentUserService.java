package com.revibe.backend.application.service;

import com.revibe.backend.application.ports.input.GetCurrentUserUseCase;
import com.revibe.backend.application.ports.output.UserRepositoryPort;
import com.revibe.backend.domain.exception.UserNotFoundException;
import com.revibe.backend.domain.model.User;
import com.revibe.backend.domain.model.UserStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetCurrentUserService implements GetCurrentUserUseCase {

    private final UserRepositoryPort userRepository;

    @Override
    @Transactional(readOnly = true)
    public User getCurrentUser(UUID userId) {
        return userRepository.findById(userId)
                .filter(user -> user.getStatus() != UserStatus.DELETED)
                .orElseThrow(UserNotFoundException::new);
    }
}
