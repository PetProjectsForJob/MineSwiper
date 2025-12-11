package kz.shift.minesweeper.repository;

import kz.shift.minesweeper.model.Game;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class InMemoryGameRepository implements GameRepository {

    private final Map<String, Game> storage = new ConcurrentHashMap<>();

    @Override
    public void save(Game game) {
        storage.put(game.getId(), game);
    }

    @Override
    public Optional<Game> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }
}
