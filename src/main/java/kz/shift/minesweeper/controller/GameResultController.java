package kz.shift.minesweeper.controller;

import kz.shift.minesweeper.model.GameResult;
import kz.shift.minesweeper.repository.GameResultRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/results")
public class GameResultController {

    private final GameResultRepository gameResultRepository;

    public GameResultController(GameResultRepository gameResultRepository) {
        this.gameResultRepository = gameResultRepository;
    }

    @GetMapping
    public List<GameResult> getResults() {
        return gameResultRepository.findAll();
    }

    @PostMapping
    public void saveResult(@RequestBody GameResult result) {
        gameResultRepository.save(result);
    }
}
