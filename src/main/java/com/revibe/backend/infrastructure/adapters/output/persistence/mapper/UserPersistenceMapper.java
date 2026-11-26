package com.revibe.backend.infrastructure.adapters.output.persistence.mapper;

import com.revibe.backend.domain.model.User;
import com.revibe.backend.infrastructure.adapters.output.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;

/**
 * Traduce entre el User del dominio y la entidad JPA. Vive en infraestructura porque conoce UserEntity.
 */
@Component
public class UserPersistenceMapper {

    public UserEntity toEntity(User user) {
        UserEntity entity = new UserEntity();
        entity.setId(user.getId());
        entity.setUsername(user.getUsername());
        entity.setEmail(user.getEmail());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setFirstName(user.getFirstName());
        entity.setLastName(user.getLastName());
        entity.setBirthDate(user.getBirthDate());
        entity.setStatus(user.getStatus());
        return entity;
    }

    public User toDomain(UserEntity entity) {
        return new User(entity.getId(), entity.getUsername(), entity.getEmail(), entity.getPasswordHash(),
                entity.getFirstName(), entity.getLastName(), entity.getBirthDate(), entity.getStatus(),
                entity.getCreatedAt());
    }
}
