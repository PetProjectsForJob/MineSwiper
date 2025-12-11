package kz.shift.minesweeper.service.impl;

import kz.shift.minesweeper.exception.GameAlreadyOverException;
import kz.shift.minesweeper.exception.GameNotFoundException;
import kz.shift.minesweeper.exception.InvalidGameOperationException;
import kz.shift.minesweeper.model.Cell;
import kz.shift.minesweeper.model.Game;
import kz.shift.minesweeper.repository.GameRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;


public class GameServiceImplTest {

    private static final int ROWS = 10;
    private static final int COLS = 10;
    private static final int MINES = 10;

    @Test
    void createGameShouldCreateGameWithCorrectDimensions() {
        GameRepository mockRepo = mock(GameRepository.class);
        GameServiceImpl service = new GameServiceImpl(mockRepo);

        Game game = service.createGame(ROWS, COLS, MINES);

        assertThat(game).isNotNull();
        assertThat(game.getRows()).isEqualTo(ROWS);
        assertThat(game.getCols()).isEqualTo(COLS);
        assertThat(game.getMinesCount()).isEqualTo(MINES);
    }

    @Test
    void createGameShouldInitializeAllCells() {
        GameRepository mockRepo = mock(GameRepository.class);
        GameServiceImpl service = new GameServiceImpl(mockRepo);

        Game game = service.createGame(ROWS, COLS, MINES);

        for (int row = 0; row < game.getRows(); row++) {
            for (int column = 0; column < game.getCols(); column++) {
                assertThat(game.getCell(row, column)).isNotNull();
                assertThat(game.getCell(row, column).isMine()).isFalse();
                assertThat(game.getCell(row, column).isRevealed()).isFalse();
                assertThat(game.getCell(row, column).isFlagged()).isFalse();
            }
        }
    }

    @Test
    void createGameShouldSaveGameInRepository() {
        GameRepository mockRepo = mock(GameRepository.class);
        GameServiceImpl service = new GameServiceImpl(mockRepo);

        Game game = service.createGame(ROWS, COLS, MINES);

        verify(mockRepo).save(game);
    }


    @Test
    void revealCellShouldThrowExceptionWhenGameNotFound() {
        GameRepository mockRepo = mock(GameRepository.class);
        GameServiceImpl service = new GameServiceImpl(mockRepo);

        when(mockRepo.findById("invalid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.revealCell("invalid", 0, 0))
                .isInstanceOf(GameNotFoundException.class)
                .hasMessageContaining("Game not found");
    }

    @Test
    void revealCellShouldReturnGameWhenGameAlreadyOver() {
        GameRepository mockRepo = mock(GameRepository.class);
        Game game = new Game("1", 3, 3, 0);
        game.setGameOver(true);

        when(mockRepo.findById("1")).thenReturn(Optional.of(game));

        GameServiceImpl service = new GameServiceImpl(mockRepo);
        Game result = service.revealCell("1", 0, 0);

        assertThat(result.isGameOver()).isTrue();
        verify(mockRepo, never()).save(any());
    }

    @Test
    void revealCellShouldPlaceMinesOnFirstReveal() {
        GameRepository mockRepo = mock(GameRepository.class);
        Game game = new Game("1", 3, 3, 2);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                game.setCell(row, column, new Cell());
            }
        }

        when(mockRepo.findById("1")).thenReturn(Optional.of(game));

        GameServiceImpl service = new GameServiceImpl(mockRepo);
        service.revealCell("1", 0, 0);

        int mineCount = 0;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                if (game.getCell(row, column).isMine()) mineCount++;
            }
        }

        assertThat(mineCount).isEqualTo(2);
        verify(mockRepo).save(game);
    }

    @Test
    void revealCellShouldReturnWhenCellAlreadyRevealedOrFlagged() {
        GameRepository mockRepo = mock(GameRepository.class);
        Game game = new Game("1", 2, 2, 1);

        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 2; column++) {
                game.setCell(row, column, new Cell());
            }
        }

        game.getCell(0, 0).setRevealed(true);

        game.getCell(1, 1).setMine(true);

        when(mockRepo.findById("1")).thenReturn(Optional.of(game));

        GameServiceImpl service = new GameServiceImpl(mockRepo);
        Game result = service.revealCell("1", 0, 0);

        assertThat(result.getCell(0, 0).isRevealed()).isTrue();
        verify(mockRepo, never()).save(any());
    }

    @Test
    void revealCellShouldEndGameWhenCellIsMine() {
        GameRepository mockRepo = mock(GameRepository.class);
        Game game = new Game("1", 2, 2, 1);

        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 2; column++) {
                game.setCell(row, column, new Cell());
            }
        }

        game.getCell(0, 0).setMine(true);

        when(mockRepo.findById("1")).thenReturn(Optional.of(game));

        GameServiceImpl service = new GameServiceImpl(mockRepo);
        Game result = service.revealCell("1", 0, 0);

        assertThat(result.isGameOver()).isTrue();
        assertThat(result.getCell(0, 0).isRevealed()).isTrue();
        verify(mockRepo).save(game);
    }

    @Test
    void revealCellShouldFloodRevealWhenCellIsSafe() throws Exception {
        GameRepository mockRepo = mock(GameRepository.class);
        Game game = new Game("1", 2, 2, 1);

        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 2; column++) {
                game.setCell(row, column, new Cell());
            }
        }

        game.getCell(1, 1).setMine(true);

        when(mockRepo.findById("1")).thenReturn(Optional.of(game));

        GameServiceImpl service = new GameServiceImpl(mockRepo);

        Field firstRevealField = GameServiceImpl.class.getDeclaredField("firstRevealPerformed");
        firstRevealField.setAccessible(true);
        Set<String> firstRevealSet;
        firstRevealSet = (Set<String>) firstRevealField.get(service);
        firstRevealSet.add("1");

        Game result = service.revealCell("1", 0, 0);

        assertThat(result.getCell(0, 0).isRevealed()).isTrue();
        assertThat(result.getCell(0, 1).isRevealed()).isTrue();
        assertThat(result.getCell(1, 0).isRevealed()).isTrue();

        assertThat(result.getCell(1, 1).isRevealed()).isFalse();

        verify(mockRepo).save(game);
    }

    @Test
    void toggleFlagShouldPlaceFlagWhenCellIsNotRevealedAndFlagsAvailable() {
        GameRepository mockRepo = mock(GameRepository.class);
        Game game = new Game("1", 2, 2, 1);
        game.setFlagCount(1);

        Cell cell = new Cell();
        game.setCell(0, 0, cell);

        when(mockRepo.findById("1")).thenReturn(Optional.of(game));

        GameServiceImpl service = new GameServiceImpl(mockRepo);
        Game result = service.toggleFlag("1", 0, 0);

        assertThat(result.getCell(0, 0).isFlagged()).isTrue();
        assertThat(result.getFlagCount()).isEqualTo(0);
        verify(mockRepo).save(game);
    }

    @Test
    void toggleFlagShouldRemoveFlagWhenCellAlreadyFlagged() {
        GameRepository mockRepo = mock(GameRepository.class);
        Game game = new Game("1", 2, 2, 1);
        game.setFlagCount(0);

        Cell cell = new Cell();
        cell.setFlagged(true);
        game.setCell(0, 0, cell);

        when(mockRepo.findById("1")).thenReturn(Optional.of(game));

        GameServiceImpl service = new GameServiceImpl(mockRepo);
        Game result = service.toggleFlag("1", 0, 0);

        assertThat(result.getCell(0, 0).isFlagged()).isFalse();
        assertThat(result.getFlagCount()).isEqualTo(1);
        verify(mockRepo).save(game);
    }

    @Test
    void toggleFlagShouldThrowWhenGameOver() {
        GameRepository mockRepo = mock(GameRepository.class);
        Game game = new Game("1", 2, 2, 1);
        game.setGameOver(true);
        game.setFlagCount(1);

        when(mockRepo.findById("1")).thenReturn(Optional.of(game));

        GameServiceImpl service = new GameServiceImpl(mockRepo);

        assertThatThrownBy(() -> service.toggleFlag("1", 0, 0))
                .isInstanceOf(GameAlreadyOverException.class)
                .hasMessage("No flag, game over");

        verify(mockRepo, never()).save(any());
    }

    @Test
    void toggleFlagShouldThrowWhenCellAlreadyRevealed() {
        GameRepository mockRepo = mock(GameRepository.class);
        Game game = new Game("1", 2, 2, 1);
        game.setFlagCount(1);

        Cell cell = new Cell();
        cell.setRevealed(true);
        game.setCell(0, 0, cell);

        when(mockRepo.findById("1")).thenReturn(Optional.of(game));

        GameServiceImpl service = new GameServiceImpl(mockRepo);

        assertThatThrownBy(() -> service.toggleFlag("1", 0, 0))
                .isInstanceOf(InvalidGameOperationException.class)
                .hasMessage("Cannot flag a revealed cell");

        verify(mockRepo, never()).save(any());
    }

    @Test
    void toggleFlagShouldEndGameWhenAllMinesFlagged() {
        GameRepository mockRepo = mock(GameRepository.class);
        Game game = new Game("1", 2, 2, 1);
        game.setFlagCount(1);

        Cell mineCell = new Cell();
        mineCell.setMine(true);
        game.setCell(0, 0, mineCell);

        when(mockRepo.findById("1")).thenReturn(Optional.of(game));

        GameServiceImpl service = new GameServiceImpl(mockRepo);
        Game result = service.toggleFlag("1", 0, 0);

        assertThat(result.isGameOver()).isTrue();
        assertThat(result.getCell(0, 0).isFlagged()).isTrue();
        verify(mockRepo).save(game);
    }

    @Test
    void toggleFlagShouldThrowWhenNoFlagsRemaining() {
        GameRepository mockRepo = mock(GameRepository.class);
        Game game = new Game("1", 2, 2, 1);
        game.setFlagCount(0);

        Cell cell = new Cell();
        game.setCell(0, 0, cell);

        when(mockRepo.findById("1")).thenReturn(Optional.of(game));

        GameServiceImpl service = new GameServiceImpl(mockRepo);

        assertThatThrownBy(() -> service.toggleFlag("1", 0, 0))
                .isInstanceOf(InvalidGameOperationException.class)
                .hasMessage("No flags remaining");

        verify(mockRepo, never()).save(any());
    }

    @Test
    void placeMinesShouldPlaceCorrectNumberOfMinesAndExcludeCell() throws Exception {
        GameRepository mockRepo = mock(GameRepository.class);
        Game game = new Game("1", 3, 3, 2);


        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                game.setCell(row, column, new Cell());
            }
        }

        Method placeMinesMethod = GameServiceImpl.class.getDeclaredMethod(
                "placeMines", Game.class, int.class, int.class, int.class);
        placeMinesMethod.setAccessible(true);

        GameServiceImpl service = new GameServiceImpl(mockRepo);

        placeMinesMethod.invoke(service, game, 2, 1, 1);

        assertThat(game.getCell(1, 1).isMine()).isFalse();

        int mineCount = 0;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                if (game.getCell(row, column).isMine()) mineCount++;
            }
        }
        assertThat(mineCount).isEqualTo(2);
    }

    @Test
    void computeAdjacentCountsShouldCalculateCorrectly() throws Exception {
        GameRepository mockRepository = mock(GameRepository.class);
        GameServiceImpl service = new GameServiceImpl(mockRepository);

        Game game = new Game("1", ROWS, COLS, MINES);

        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLS; column++) {
                game.setCell(row, column, new Cell());
            }
        }

        game.getCell(0, 0).setMine(true);
        game.getCell(1, 1).setMine(true);

        Method method = GameServiceImpl.class.getDeclaredMethod("computeAdjacentCounts", Game.class);
        method.setAccessible(true);
        method.invoke(service, game);

        assertThat(game.getCell(0, 0).getAdjacentMines()).isEqualTo(-1);
        assertThat(game.getCell(0, 1).getAdjacentMines()).isEqualTo(2);
        assertThat(game.getCell(1, 0).getAdjacentMines()).isEqualTo(2);
        assertThat(game.getCell(2, 2).getAdjacentMines()).isEqualTo(1);
    }

    @Test
    void floodRevealShouldRevealAllSafeCells() throws Exception {
        GameRepository mockRepo = mock(GameRepository.class);
        Game game = new Game("1", 3, 3, 1);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                game.setCell(row, column, new Cell());
            }
        }

        game.getCell(2, 2).setMine(true);

        GameServiceImpl service = new GameServiceImpl(mockRepo);

        Method computeMethod = GameServiceImpl.class.getDeclaredMethod("computeAdjacentCounts", Game.class);
        computeMethod.setAccessible(true);
        computeMethod.invoke(service, game);

        Method floodMethod = GameServiceImpl.class.getDeclaredMethod("floodReveal", Game.class, int.class, int.class);
        floodMethod.setAccessible(true);
        floodMethod.invoke(service, game, 0, 0);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                if (!game.getCell(row, column).isMine()) {
                    assertThat(game.getCell(row, column).isRevealed()).isTrue();
                }
            }
        }

        assertThat(game.getCell(2, 2).isRevealed()).isFalse();
    }
}
