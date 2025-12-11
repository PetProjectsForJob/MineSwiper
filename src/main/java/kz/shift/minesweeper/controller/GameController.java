package kz.shift.minesweeper.controller;

import kz.shift.minesweeper.service.GameService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import kz.shift.minesweeper.model.Game;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;


@RestController
@RequestMapping("/api")
@Tag(name = "Game API", description = "API для игры Сапер")
public class GameController {

    private static final int DEFAULT_ROWS = 10;
    private static final int DEFAULT_COLS = 10;
    private static final int DEFAULT_MINES = 10;

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping("/game")
    @Operation(summary = "Начать новую игру", description = "Создает новую игру с размером 10x10 и 10 минами")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Игра успешно создана")
    })
    public Game startNewGame() {
        return gameService.createGame(DEFAULT_ROWS, DEFAULT_COLS, DEFAULT_MINES);
    }

    @PostMapping("/game/{id}/reveal")
    @Operation(summary = "Открыть клетку", description = "Открывает клетку на игровом поле")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Клетка открыта"),
        @ApiResponse(responseCode = "404", description = "Игра не найдена")
    })
    public Game revealCell(@PathVariable("id") @Parameter(description = "ID игры") String id,
                           @RequestParam @Parameter(description = "Номер строки") int row,
                           @RequestParam @Parameter(description = "Номер столбца") int col) {
        return gameService.revealCell(id, row, col);
    }

    @PostMapping("/game/{id}/toggle-flag")
    @Operation(summary = "Установить/снять флаг", description = "Переключает флаг на клетке")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Флаг переключен"),
        @ApiResponse(responseCode = "400", description = "Недопустимая операция"),
        @ApiResponse(responseCode = "404", description = "Игра не найдена")
    })
    public Game toggleFlag(@PathVariable("id") @Parameter(description = "ID игры") String id,
                           @RequestParam @Parameter(description = "Номер строки") int row,
                           @RequestParam @Parameter(description = "Номер столбца") int col) {
        return gameService.toggleFlag(id, row, col);
    }
}
