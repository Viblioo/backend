package com.revibe.backend.application.ports.output;

public interface PasswordHasherPort {
    String hash(String rawPassword);
}
