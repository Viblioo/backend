package com.revibe.backend.application.ports.input;

import com.revibe.backend.domain.model.User;
import java.util.UUID;

public interface GetCurrentUserUseCase {

    /** El usuario de la sesión actual. UserNotFoundException si ya no existe o está borrado. */
    User getCurrentUser(UUID userId);
}
