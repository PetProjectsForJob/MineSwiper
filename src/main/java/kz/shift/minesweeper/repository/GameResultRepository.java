package kz.shift.minesweeper.repository;

import kz.shift.minesweeper.model.GameResult;

import java.util.List;

public interface GameResultRepository {

    void save(GameResult result);

    List<GameResult> findAll();
}
