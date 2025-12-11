package kz.shift.minesweeper.service;

import kz.shift.minesweeper.model.Game;

public interface GameService {

    Game createGame(int rows, int cols, int mines);

    Game revealCell(String gameId, int row, int col);

    Game toggleFlag(String gameId, int row, int col);
}
