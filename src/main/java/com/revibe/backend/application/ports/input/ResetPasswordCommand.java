package com.revibe.backend.application.ports.input;

public record ResetPasswordCommand(String email, String code, String newPassword) {

    @Override
    public String toString() {
        return "ResetPasswordCommand[email=%s]".formatted(email);
    }
}
