package kz.shift.minesweeper.repository;

import kz.shift.minesweeper.model.Game;

import java.util.Optional;

public interface GameRepository {

    void save(Game game);

    Optional<Game> findById(String id);
}
