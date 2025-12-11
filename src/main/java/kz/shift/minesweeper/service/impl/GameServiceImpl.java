package kz.shift.minesweeper.service.impl;

import kz.shift.minesweeper.exception.GameAlreadyOverException;
import kz.shift.minesweeper.exception.GameNotFoundException;
import kz.shift.minesweeper.exception.InvalidGameOperationException;
import kz.shift.minesweeper.model.Cell;
import kz.shift.minesweeper.model.Game;
import kz.shift.minesweeper.repository.GameRepository;
import kz.shift.minesweeper.service.GameService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@Service
public class GameServiceImpl implements GameService {

    private static final int FLAG_INCREMENT = 1;
    private static final int FLAG_DECREMENT = -1;
    private static final int MINE_CELL = -1;
    private static final int INITIAL_ADJACENT_MINES = 0;
    private static final int NO_ADJACENT_MINES = 0;
    private static final int NEIGHBOR_OFFSET = 1;

    private final GameRepository gameRepository;
    private final Set<String> firstRevealPerformed = Collections.synchronizedSet(new HashSet<>());
    private final Random random = new Random();

    public GameServiceImpl(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @Override
    public Game createGame(int rows, int columns, int mines) {

        String gameId = UUID.randomUUID().toString();
        Game game = new Game(gameId, rows, columns, mines);

        log.info("Create new game with ID: {}, rows: {}, columns: {}, mines: {}", gameId, rows, columns, mines);

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                game.setCell(row, column, new Cell());
            }
        }

        gameRepository.save(game);
        return game;
    }

    @Override
    public Game revealCell(String gameId, int row, int col) {

        Game game = findGameOrThrow(gameId);

        if (game.isGameOver()) {
            log.warn("Try to open the cell in a completed game: {}", gameId);
            return game;
        }

        if (!firstRevealPerformed.contains(gameId)) {
            log.info("First disclosure, placing mines for play {}", gameId);
            placeMines(game, game.getMinesCount(), row, col);
            computeAdjacentCounts(game);
            firstRevealPerformed.add(gameId);
        }

        Cell cell = game.getCell(row, col);

        if (cell.isFlagged() || cell.isRevealed()) {
            return game;
        }

        if (cell.isMine()) {
            cell.setRevealed(true);
            game.setGameOver(true);
            log.warn("The player crashed on a mine! Game ID: {}, cell: ({},{})", gameId, row, col);
            gameRepository.save(game);
            return game;
        }

        floodReveal(game, row, col);

        gameRepository.save(game);
        return game;
    }

    @Override
    public Game toggleFlag(String gameId, int row, int col) {

        Game game = findGameOrThrow(gameId);

        Cell cell = game.getCell(row, col);

        if (game.isGameOver()) {
            log.warn("Try to put a flag in the completed game {}", gameId);
            throw new GameAlreadyOverException("No flag, game over");
        }

        if (cell.isRevealed()) {
            log.warn("Attempt to put the flag on an open Cell ({},{}) in game {}", row, col, gameId);
            throw new InvalidGameOperationException("Cannot flag a revealed cell");
        }

        boolean newFlag = !cell.isFlagged();

        if (newFlag && game.getFlagCount() <= 0) {
            log.warn("No available game flags {}", gameId);
            throw new InvalidGameOperationException("No flags remaining");
        }

        updateFlag(game, cell, newFlag);
        log.info("Flag {} on the cell ({},{}) in game {}", newFlag ? "installed" : "removed", row, col, gameId);

        cell.setFlagged(newFlag);

        if (game.getFlaggedMines() == game.getMinesCount()) {
            log.info("All mines found, game finished. Game ID: {}", gameId);
            game.setGameOver(true);
        }

        gameRepository.save(game);
        return game;
    }

    private void updateFlag(Game game, Cell cell, boolean placingFlag) {

        cell.setFlagged(placingFlag);
        game.setFlagCount(game.getFlagCount() + (placingFlag ? FLAG_DECREMENT : FLAG_INCREMENT));
        if (cell.isMine()) {
            game.setFlaggedMines(game.getFlaggedMines() + (placingFlag ? FLAG_INCREMENT : FLAG_DECREMENT));
        }
    }

    private Game findGameOrThrow(String gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> {
                    log.error("Game not found: {}", gameId);
                    return new GameNotFoundException("Game not found");
                });
    }

    private void placeMines(Game game, int minesCount, int excludeRow, int excludeColumn) {

        int rows = game.getRows();
        int columns = game.getCols();
        List<int[]> availableCells = new ArrayList<>();

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                if (row == excludeRow && column == excludeColumn) continue;
                availableCells.add(new int[]{row, column});
            }
        }

        Collections.shuffle(availableCells, random);

        for (int i = 0; i < Math.min(minesCount, availableCells.size()); i++) {
            int[] pos = availableCells.get(i);
            game.getCell(pos[0], pos[1]).setMine(true);
        }
    }

    private void computeAdjacentCounts(Game game) {

        int rows = game.getRows();
        int columns = game.getCols();

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                Cell cell = game.getCell(row, column);

                if (cell.isMine()) {
                    cell.setAdjacentMines(MINE_CELL);
                } else {
                    int count = INITIAL_ADJACENT_MINES;

                    for (int nextRow = row - NEIGHBOR_OFFSET; nextRow <= row + NEIGHBOR_OFFSET; nextRow++) {
                        for (int nextColumn = column - NEIGHBOR_OFFSET; nextColumn <= column + NEIGHBOR_OFFSET; nextColumn++) {
                            if ((nextRow != row || nextColumn != column)
                                    && nextRow >= 0 && nextRow < rows
                                    && nextColumn >= 0 && nextColumn < columns
                                    && game.getCell(nextRow, nextColumn).isMine()) {
                                count++;
                            }
                        }
                    }
                    cell.setAdjacentMines(count);
                }
            }
        }
    }

    private void floodReveal(Game game, int startRow, int startColumn) {

        int rows = game.getRows();
        int columns = game.getCols();
        Deque<int[]> deque = new ArrayDeque<>();

        deque.add(new int[]{startRow, startColumn});

        while (!deque.isEmpty()) {

            int[] position = deque.poll();
            int row = position[0];
            int column = position[1];
            Cell cell = game.getCell(row, column);

            if (cell.isRevealed() || cell.isFlagged()) continue;
            cell.setRevealed(true);

            if (cell.getAdjacentMines() == NO_ADJACENT_MINES) {
                for (int nextRow = row - NEIGHBOR_OFFSET; nextRow <= row + NEIGHBOR_OFFSET; nextRow++) {
                    for (int nextColumn = column - NEIGHBOR_OFFSET; nextColumn <= column + NEIGHBOR_OFFSET; nextColumn++) {
                        if (nextRow >= 0 && nextRow < rows && nextColumn >= 0 && nextColumn < columns) {

                            Cell neighbor = game.getCell(nextRow, nextColumn);
                            if (!neighbor.isRevealed() && !neighbor.isMine()) {
                                deque.add(new int[]{nextRow, nextColumn});
                            }
                        }
                    }
                }
            }
        }
    }
}
