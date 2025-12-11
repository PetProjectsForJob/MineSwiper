package kz.shift.minesweeper.exception;

public class InvalidGameOperationException extends RuntimeException {
  public InvalidGameOperationException(String message) {
    super(message);
  }
}
