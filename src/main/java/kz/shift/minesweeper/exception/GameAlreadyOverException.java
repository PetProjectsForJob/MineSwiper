package kz.shift.minesweeper.exception;

public class GameAlreadyOverException extends RuntimeException {
    public GameAlreadyOverException(String message) {
        super(message);
    }
}
