package dev.manestack.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GameService {
    public Uni<Void> createTable() {
        return Uni.createFrom().voidItem();
    }
}
