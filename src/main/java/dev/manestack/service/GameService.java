package dev.manestack.service;

import dev.manestack.service.game.core.common.AbstractGameTable;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class GameService {
    public static final Map<String, AbstractGameTable> GAME_SESSIONS = new HashMap<>();
}
