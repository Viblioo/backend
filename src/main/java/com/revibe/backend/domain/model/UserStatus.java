package com.revibe.backend.domain.model;

public enum UserStatus {
    PENDING("pending"), ACTIVE("active"), SUSPENDED("suspended"), DELETED("deleted");

    UserStatus(String value) {
        this.value = value;
    }

    String value;
}
