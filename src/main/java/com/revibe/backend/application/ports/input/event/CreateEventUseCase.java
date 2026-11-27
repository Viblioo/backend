package com.revibe.backend.application.ports.input.event;

public interface CreateEventUseCase {

    CreateEventResult create(CreateEventCommand command);
}
