package com.revibe.backend.application.ports.output.auth;

public interface EmailSenderPort {

    void sendActivation(String to, String code);

    void sendPasswordReset(String to, String code);
}
