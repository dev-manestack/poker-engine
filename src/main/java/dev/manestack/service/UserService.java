package dev.manestack.service;

import dev.manestack.service.dto.UserDTO;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserService {

    public Uni<Void> createUser(UserDTO userDTO) {
        return Uni.createFrom().voidItem();
    }
}
