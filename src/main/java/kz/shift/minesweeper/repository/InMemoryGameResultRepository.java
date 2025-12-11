package kz.shift.minesweeper.repository;

import kz.shift.minesweeper.model.GameResult;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Repository
public class InMemoryGameResultRepository implements GameResultRepository {

    private final List<GameResult> storageResult = new ArrayList<>();

    @Override
    public synchronized void save(GameResult result) {
        storageResult.add(result);
    }

    @Override
    public List<GameResult> findAll() {
        return Collections.unmodifiableList(storageResult);
    }
}

