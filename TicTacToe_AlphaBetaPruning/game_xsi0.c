#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <ctype.h>
#include <omp.h>

#define DEBUG 0
#define LOWER_LIMIT -1000
#define UPPER_LIMIT 1000
#define NUMTHREADS 8
#define MAXDEPTH 7

typedef struct {
    int size;
    char **cells;
} Board;

// Function to initialize the board with empty spaces
void initializeBoard(Board *board) {
    for (int i = 0; i < board->size; i++) {
        for (int j = 0; j < board->size; j++) {
            board->cells[i][j] = ' ';
        }
    }
}


void printBoard(Board *board) {
    printf("\n    "); // Initial spacing for row numbers

    // Print column numbers at the top
    for (int col = 1; col <= board->size; col++) {
        printf(" %d  ", col);
    }
    printf("\n");

    for (int i = 0; i < board->size; i++) {
        printf(" %2d ", i + 1); // Print row number on the left
        for (int j = 0; j < board->size; j++) {
            printf(" %c ", board->cells[i][j]);
            if (j < board->size - 1) printf("|");
        }
        printf("\n");
        
        // Print horizontal separators between rows
        if (i < board->size - 1) {
            printf("    "); // Align separators with column numbers
            for (int k = 0; k < board->size; k++) {
                printf("---");
                if (k < board->size - 1) printf("|");
            }
            printf("\n");
        }
    }
    printf("\n");
}

// Function to check if a player has won
int checkWin(Board *board, char player) {
    for (int i = 0; i < board->size; i++) {
        int rowWin = 1, colWin = 1;
        for (int j = 0; j < board->size; j++) {
            if (board->cells[i][j] != player) rowWin = 0;
            if (board->cells[j][i] != player) colWin = 0;
        }
        if (rowWin || colWin) return 1;
    }
    int diag1Win = 1, diag2Win = 1;
    for (int i = 0; i < board->size; i++) {
        if (board->cells[i][i] != player) diag1Win = 0;
        if (board->cells[i][board->size - 1 - i] != player) diag2Win = 0;
    }
    return diag1Win || diag2Win;
}

// Function to check if the board is full
int isBoardFull(Board *board) {
    for (int i = 0; i < board->size; i++) {
        for (int j = 0; j < board->size; j++) {
            if (board->cells[i][j] == ' ') return 0;
        }
    }
    return 1;
}

Board *boardClone(Board *board){
    Board *newBoard = (Board *)malloc(sizeof(Board));
    newBoard->size = board->size;
    newBoard->cells = (char **)malloc(newBoard->size * sizeof(char *));
    for (int i = 0; i < newBoard->size; i++) {
        newBoard->cells[i] = (char *)malloc(newBoard->size * sizeof(char));
        for (int j = 0; j < newBoard->size; j++) {
            newBoard->cells[i][j] = board->cells[i][j];
        }
    }
    return newBoard;
}

void freeAllocatedBoard(Board *board){
    for (int i = 0; i < board->size; i++) {
        free(board->cells[i]);
    }
    free(board->cells);
    free(board);
}

void playerMove(Board *board) {
    int row, col;
    while (1) {
        printf("Enter your move (row number from 1 to %d): ", board->size);
        if (scanf("%d", &row) != 1 || row < 1 || row > board->size) {
            printf("Invalid input. Please enter a valid row number.\n");
            while (getchar() != '\n'); // Clear the input buffer
            continue;
        }

        printf("Enter your move (column number from 1 to %d): ", board->size);
        if (scanf("%d", &col) != 1 || col < 1 || col > board->size) {
            printf("Invalid input. Please enter a valid column number.\n");
            while (getchar() != '\n'); // Clear the input buffer
            continue;
        }
        
        row--; // Adjust for 0-indexed array
        col--;
        if (board->cells[row][col] == ' ') {
            board->cells[row][col] = 'X';
            break;
        } else {
            printf("Cell already taken. Try again.\n");
        }
    }
}

int negamax(Board *board, int depth, int alpha, int beta, int maxDepth, int color) {

    if (checkWin(board, 'O')) return  (10 - depth) * color;
    if (checkWin(board, 'X')) return  (depth - 10) * color;
    if (isBoardFull(board) || depth == maxDepth) return 0;

    for (int i = 0; i < board->size; i++) {
        for (int j = 0; j < board->size; j++) {
            if (board->cells[i][j] == ' ') {
                // Make move by color
                board->cells[i][j] = (color == +1 ? 'O' : 'X');

                // negamax quirk, color is flipped for opponent
                int eval = -negamax(board, depth + 1, -beta, -alpha, maxDepth, -color);
                board->cells[i][j] = ' ';

                alpha = (eval > alpha) ? eval : alpha;
                if (alpha >= beta) {
                    return alpha;
                }
            }
        }
    }
    return alpha;
}


void computerMove(Board *board, char symbol) {
    int bestScore = LOWER_LIMIT;
    int moveRow = -1, moveCol = -1;
    int maxDepth = MAXDEPTH;

    int alpha = LOWER_LIMIT;
    int beta  = UPPER_LIMIT;

    // +1 means 'O' is maximizing, -1 means 'X' is maximizing
    int color = (symbol == 'O') ? 1 : -1;

    for (int i = 0; i < board->size; i++) {
        for (int j = 0; j < board->size; j++) {
            if (board->cells[i][j] == ' ') {
                board->cells[i][j] = symbol;

                int score = -negamax(board, 1, -beta, -alpha, maxDepth, -color);

                board->cells[i][j] = ' ';

                if (score > bestScore) {
                    bestScore = score;
                    moveRow = i;
                    moveCol = j;
                }

                alpha = (score > alpha) ? score : alpha;
            }
        }
    }
    board->cells[moveRow][moveCol] = symbol;
}

void computerMoveParallel(Board *board, char symbol) {
    int globalBestScore = LOWER_LIMIT;
    int globalMoveRow = -1, globalMoveCol = -1;
    int maxDepth = MAXDEPTH;

    int globalAlpha = LOWER_LIMIT;
    int beta  = UPPER_LIMIT;

    // +1 means 'O' is maximizing, -1 means 'X' is maximizing
    int color = (symbol == 'O') ? 1 : -1;

    Board *boards[NUMTHREADS];
    for (int i = 0; i < NUMTHREADS; i++) {
        boards[i] = boardClone(board);
    }

    #pragma omp parallel num_threads(NUMTHREADS) default(none) shared(boards, maxDepth, color, board, symbol, globalBestScore, globalMoveRow, globalMoveCol, globalAlpha) firstprivate(beta)
    {
        int localBestScore = LOWER_LIMIT;
        int localMoveRow = -1, localMoveCol = -1;
        int alpha = LOWER_LIMIT, snapshot;
        int rank = omp_get_thread_num();

        #pragma omp for schedule(static) collapse(2)
        for (int i = 0; i < board->size; i++) {
            for (int j = 0; j < board->size; j++) {

                #pragma omp atomic read
                snapshot = globalAlpha;
                alpha = snapshot;

                if (boards[rank]->cells[i][j] == ' ') {
                    boards[rank]->cells[i][j] = symbol;

                    // 1 cause it's opponent's turn
                    int score = -negamax(boards[rank], 1, -beta, -alpha, maxDepth, -color);

                    boards[rank]->cells[i][j] = ' ';

                    if (score > localBestScore) {
                        localBestScore = score;
                        localMoveRow = i;
                        localMoveCol = j;
                    }

                    alpha = (score > alpha) ? score : alpha;
                    #pragma omp critical
                    {
                        if (alpha > globalAlpha)
                            globalAlpha = alpha;
                    }
                }
            }
        }
        #pragma omp critical
        {
            if (localBestScore > globalBestScore) {
                globalBestScore = localBestScore;
                globalMoveRow = localMoveRow;
                globalMoveCol = localMoveCol;
            }
        }
    }
    board->cells[globalMoveRow][globalMoveCol] = symbol;

    for (int i = 0; i < NUMTHREADS; i++) {
        freeAllocatedBoard(boards[i]);
    }
}

void playHumanVsComputer(Board *board, int parallel, char *winner, int *moves){
    while (1) {
        printBoard(board);
        playerMove(board);
        (*moves)++;
        if (checkWin(board, 'X')) {
            *winner = 'X';
            break;
        }
        if (isBoardFull(board)) {
            break;
        }

        if (parallel){
            computerMoveParallel(board, 'O');
            (*moves)++;
        } else {
            computerMove(board, 'O');
            (*moves)++;
        }

        if (checkWin(board, 'O')) {
            *winner = 'O';
            break;
        }
        if (isBoardFull(board)) {
            break;
        }
    }
}


void playComputerVsHuman(Board *board, int parallel, char *winner, int *moves){
    while (1){
        if (parallel) {
            computerMoveParallel(board, 'X');
            (*moves)++;
        } else {
            computerMove(board, 'X');
            (*moves)++;
        }
        printBoard(board);

        if (checkWin(board, 'X')) {
            *winner = 'X';
            break;
        }

        if (isBoardFull(board)) {
            break;
        }

        playerMove(board);
        (*moves)++;
        if (checkWin(board, 'O')) {
            *winner = 'O';
            break;
        }
        if (isBoardFull(board)) {
            break;
        }
    }
}


/*
If N*N is odd → the first player (X) makes the last move. The board can become full right after X.

If N*N is even → the second player (O) makes the last move. The board can become full right after O.
*/
void playComputerVsComputer(Board *board, int parallel, char *winner){
    while(1){
        if (parallel) {
            computerMoveParallel(board, 'X');
        } else {
            computerMove(board, 'X');
        }
        printBoard(board);

        if (checkWin(board, 'X')) {
            *winner = 'X';
            break;
        }

        if (isBoardFull(board)) {
            break;
        }

        if (parallel){
            computerMoveParallel(board, 'O');
        } else {
            computerMove(board, 'O');
        }
        printBoard(board);

        if (checkWin(board, 'O')) {
            *winner = 'O';
            break;
        }
        if (isBoardFull(board)) {
            break;
        }
    }
}


int main() {
    srand(time(NULL)); // Seed the random number generator

    Board board;
    printf("Enter the size of the board (N): ");
    while (1) {
        scanf("%d", &board.size);
        if (board.size <= 1 || board.size > 17) {
            printf("Invalid size. Please enter a number between 2 and 17: ");
            continue;
        }
        break;
    }

    board.cells = (char **)malloc(board.size * sizeof(char *));
    for (int i = 0; i < board.size; i++) {
        board.cells[i] = (char *)malloc(board.size * sizeof(char));
    }

    initializeBoard(&board);
    printf("\nStarting a %d x %d Tic-Tac-Toe game with a total of %d cells.\n", board.size, board.size, board.size * board.size);

    char winner = ' ';
    int moves = 0;


    int scenario, parallelChoice;
    printf("Choose game mode:\n");
    printf("1. Human vs Computer (Human goes first)\n");
    printf("2. Computer vs Human (Computer goes first)\n");
    printf("3. Computer vs Computer\n");
    printf("Enter choice: ");
    while (1) {
        scanf("%d", &scenario);
        if (scenario < 1 || scenario > 3) {
            printf("Invalid input. Please enter a number between 1 and 3: ");
            continue;
        }
        break;
    }

    printf("Use parallelism? (0 = No, 1 = Yes): ");
    while (1) {
        scanf("%d", &parallelChoice);
        if (parallelChoice < 0 || parallelChoice > 1) {
            printf("Invalid input. Enter 0 or 1: ");
            continue;
        }
        break;
    }

    switch (scenario) {
        case 1:
            playHumanVsComputer(&board, parallelChoice, &winner, &moves);
            printBoard(&board);
            if (winner == 'X') {
                printf("Player wins!\n");
            } else if (winner == 'O') {
                printf("Computer wins!\n");
            } else {
                printf("It's a draw!\n");
            }
            break;
        case 2:
            playComputerVsHuman(&board, parallelChoice, &winner, &moves);
            printBoard(&board);
            if (winner == 'X') {
                printf("Computer wins!\n");
            } else if (winner == 'O') {
                printf("Player wins!\n");
            } else {
                printf("It's a draw!\n");
            }
            break;
        case 3:
            playComputerVsComputer(&board, parallelChoice, &winner);
            if (winner == 'X') {
                printf("Computer X wins!\n");
            } else if (winner == 'O') {
                printf("Computer O wins!\n");
            } else {
                printf("It's a draw!\n");
            }
            break;
        default:
            printf("Invalid option.\n");
            break;
    }


    for (int i = 0; i < board.size; i++) {
        free(board.cells[i]);
    }
    free(board.cells);

    return 0;
}
