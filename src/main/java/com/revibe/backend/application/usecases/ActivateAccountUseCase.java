package com.revibe.backend.application.usecases;

public interface ActivateAccountUseCase {

    /** Pasa el usuario del código de PENDING a ACTIVE. Lanza InvalidActivationCodeException si el código no vale. */
    void activate(String code);
}
