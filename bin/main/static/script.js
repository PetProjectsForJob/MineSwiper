const API_URL = window.location.hostname === 'localhost'
    ? 'http://localhost:8080/api'  // локальный бэк
    : '/api';                      // продакшн (Render)
let gameId;
let rows, cols;
let board = [];
let gameOver = false;
let moves = 0;

async function initGame() {
    const modal = document.getElementById('gameOverModal');
    modal.style.display = 'none';
    board = [];
    gameOver = false;
    moves = 0;
    const response = await fetch(`${API_URL}/game`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
    });
    const data = await response.json();
    gameId = data.id;
    rows = 10;
    cols = 10;
    createBoard();
    updateBoard(data.board);
    updateFlagCount(data.flagCount);
}

async function revealCell(row, col) {
    if (gameOver) {
        return;
    }
    const response = await fetch(`${API_URL}/game/${gameId}/reveal?row=${row}&col=${col}`, {
        method: 'POST',
    });
    const data = await response.json();
    moves++;
    if (data.gameOver) {
        gameOver = true;
        showGameOverModal();
    }

    updateBoard(data.board, row, col);
}

function createBoard() {
    const boardElement = document.getElementById('board');
    boardElement.innerHTML = '';
    boardElement.style.gridTemplateColumns = `repeat(${cols}, 30px)`;
    boardElement.style.gridTemplateRows = `repeat(${rows}, 30px)`;

    for (let r = 0; r < rows; r++) {
        board[r] = [];
        for (let c = 0; c < cols; c++) {
            const cell = document.createElement('div');
            cell.classList.add('cell');

            // Левый клик - раскрыть клетку
            cell.addEventListener('click', () => revealCell(r, c));

            // Правый клик - поставить или снять флаг
            cell.addEventListener('contextmenu', (e) => {
                e.preventDefault();
                toggleFlag(r, c);
            });

            board[r][c] = { element: cell, revealed: false, flagged: false, mine: false };
            boardElement.appendChild(cell);
        }
    }
}

async function toggleFlag(row, col) {
    if (gameOver) {
        return;
    }
    const response = await fetch(`${API_URL}/game/${gameId}/toggle-flag?row=${row}&col=${col}`, {
        method: 'POST',
    });
    const data = await response.json();
    updateBoard(data.board);
    updateFlagCount(data.flagCount);
    moves++;

    if (data.flaggedMines === data.minesCount) {
        alert('Вы победили!');
        await saveGameResult(true);
        board = [];
        initGame();
    }
}

async function saveGameResult(won) {
    const remainingFlags = parseInt(document.getElementById('flagCount').textContent);

    await fetch(`${API_URL}/results`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            playerName: 'Игрок', // можно заменить на input-поле
            won,
            moves,
            remainingFlags,
            timestamp: new Date().toISOString()
        })
    });

    loadResults(); // обновляем список после сохранения
}


async function loadResults() {
    const res = await fetch(`${API_URL}/results`);
    const results = await res.json();
    const list = document.getElementById('resultsList');
    list.innerHTML = '';

    results.slice().reverse().forEach(result => {
        const li = document.createElement('li');
        const date = new Date(result.timestamp).toLocaleString();
        li.textContent = `${date} – ${result.won ? 'Победа 🎉' : 'Поражение 💥'} – Ходов: ${result.moves}, Флаги: ${result.remainingFlags}`;
        list.appendChild(li);
    });
}


function updateBoard(boardData, row, col) {
    for (let r = 0; r < rows; r++) {
        for (let c = 0; c < cols; c++) {
            const cellData = boardData[r][c];
            const cell = board[r][c].element;

            if (gameOver && cellData.mine && !cellData.flagged) {
                if (row === r && col === c) {
                    cell.textContent = '💥';
                } else {
                    cell.textContent = '💣';
                }

            } else if (cellData.revealed) {
                cell.classList.add('revealed');
                if (cellData.adjacentMines > 0) {
                    cell.textContent = cellData.adjacentMines;
                }
            } else if (cellData.flagged) {
                cell.classList.add('flagged');
                cell.textContent = '🚩'; // Отображаем флаг
            } else {
                cell.classList.remove('flagged');
                cell.textContent = '';
            }
        }
    }
}

function updateFlagCount(count) {
    const flagCountElement = document.getElementById('flagCount');
    flagCountElement.textContent = count;
}

function showGameOverModal() {
    saveGameResult(false);
    const modal = document.getElementById('gameOverModal');
    modal.style.display = 'flex';
}

function closeModal() {
    const modal = document.getElementById('gameOverModal');
    modal.style.display = 'none';
}

document.getElementById("toggleResults").addEventListener("click", function () {
    const panel = document.getElementById("results");
    panel.classList.toggle("open");

    this.textContent = (panel.classList.contains("open") ? "▼" : "▶") + " История игр";
});

window.onload = initGame;
window.addEventListener('DOMContentLoaded', () => {
    loadResults();
});