#define _POSIX_C_SOURCE 200112L
#include "utils.h"
#include <time.h>

typedef struct{
    int bestScore;
    int moveRow;
    int moveCol;
    //char pad[64 - 3 * sizeof(int)];  // Supposed padding to avoid false sharing, no difference in practice 
} ScoreMovePair_t;

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

void playerMove(Board *board, char symbol) {
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
            board->cells[row][col] = symbol;
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

void computerMove_DEPRECATED(Board *board, char symbol) {
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

                int score = -negamax(board, 0, -beta, -alpha, maxDepth, -color);

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

void computerMoveParallel_DEPRECATED(Board *board, char symbol) {
    int globalBestScore = LOWER_LIMIT;
    int globalMoveRow = -1, globalMoveCol = -1;

    int globalAlpha = LOWER_LIMIT;
    int beta  = UPPER_LIMIT;

    // +1 means 'O' is maximizing, -1 means 'X' is maximizing
    int color = (symbol == 'O') ? 1 : -1;

    Board *boards[NUMTHREADS];
    for (int i = 0; i < NUMTHREADS; i++) {
        boards[i] = boardClone(board);
    }

    #pragma omp parallel num_threads(NUMTHREADS) default(none) shared(boards, color, board, symbol, globalBestScore, globalMoveRow, globalMoveCol, globalAlpha) firstprivate(beta)
    {
        int localBestScore = LOWER_LIMIT;
        int localMoveRow = -1, localMoveCol = -1;
        int alpha = LOWER_LIMIT;
        int rank = omp_get_thread_num();

        #pragma omp for schedule(dynamic) collapse(2)
        for (int i = 0; i < board->size; i++) {
            for (int j = 0; j < board->size; j++) {

                #pragma omp atomic read 
                alpha = globalAlpha;

                if (boards[rank]->cells[i][j] == ' ') {
                    boards[rank]->cells[i][j] = symbol;

                    int score = -negamax(boards[rank], 0, -beta, -alpha, MAXDEPTH, -color);

                    boards[rank]->cells[i][j] = ' ';

                    if (score > localBestScore) {
                        localBestScore = score;
                        localMoveRow = i;
                        localMoveCol = j;
                    }

                    alpha = (score > alpha) ? score : alpha;
                    
                    if (alpha > globalAlpha)
                    {
                        #pragma omp atomic write    
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

int minimax(Board *board, int depth, int isMaximizing, int alpha, int beta, int maxDepth, int color) {
    if (checkWin(board, 'O')) return (10 - depth) * color;
    if (checkWin(board, 'X')) return (depth - 10) * color;
    if (isBoardFull(board) || depth == maxDepth) return 0;

    if (isMaximizing) {
        int bestScore = LOWER_LIMIT;
        for (int i = 0; i < board->size; i++) {
            for (int j = 0; j < board->size; j++) {
                if (board->cells[i][j] == ' ') {
                    board->cells[i][j] = (color == 1) ? 'O' : 'X';
                    int score = minimax(board, depth + 1, 0, alpha, beta, maxDepth, color);
                    board->cells[i][j] = ' ';
                    bestScore = score > bestScore ? score : bestScore;
                    alpha = alpha > bestScore ? alpha : bestScore;
                    if (beta <= alpha) return bestScore;
                }
            }
        }
        return bestScore;
    } else {
        int bestScore = UPPER_LIMIT;
        for (int i = 0; i < board->size; i++) {
            for (int j = 0; j < board->size; j++) {
                if (board->cells[i][j] == ' ') {
                    board->cells[i][j] = (color == 1) ? 'X' : 'O';
                    int score = minimax(board, depth + 1, 1, alpha, beta, maxDepth, color);
                    board->cells[i][j] = ' ';
                    bestScore = score < bestScore ? score : bestScore;
                    beta = beta < bestScore ? beta : bestScore;
                    if (beta <= alpha) return bestScore;
                }
            }
        }
        return bestScore;
    }
}

void computerMove(Board *board, char symbol, int *totalMoves, int maxDepth) {
    int bestScore = LOWER_LIMIT;
    int moveRow = -1, moveCol = -1;

    int color = (symbol == 'O') ? 1 : -1;

    for (int i = 0; i < board->size; i++) {
        for (int j = 0; j < board->size; j++) {
            if (board->cells[i][j] == ' ') {
                board->cells[i][j] = symbol;
                int score = minimax(board, 0, 0, LOWER_LIMIT, UPPER_LIMIT, maxDepth, color);
                board->cells[i][j] = ' ';
                if (score > bestScore) {
                    bestScore = score;
                    moveRow = i;
                    moveCol = j;
                }
            }
        }
    }
    board->cells[moveRow][moveCol] = symbol;
    (*totalMoves)++;
}

void computerMoveParallelV1(Board *board, char symbol, int *totalMoves, int numThreads, int maxDepth){

    int color = (symbol == 'O') ? 1 : -1;

    Board *boards[NUMTHREADS];
    for (int i = 0; i < NUMTHREADS; i++) {
        boards[i] = boardClone(board);
    }

    int globalMoveRow = -1, globalMoveCol = -1;
    int globalBestScore = LOWER_LIMIT;

    ScoreMovePair_t **scoresMoves = (ScoreMovePair_t **)malloc(NUMTHREADS * sizeof(ScoreMovePair_t *));
    for (int i = 0; i < NUMTHREADS; i++) {
        scoresMoves[i] = (ScoreMovePair_t *)malloc(sizeof(ScoreMovePair_t));
        scoresMoves[i]->bestScore = LOWER_LIMIT;
        scoresMoves[i]->moveRow = -1;
        scoresMoves[i]->moveCol = -1;
    }

    #pragma omp parallel num_threads(numThreads) default(none) shared(board, symbol, color, boards, globalMoveRow, globalMoveCol, globalBestScore, scoresMoves, totalMoves, maxDepth)
    {
        int rank = omp_get_thread_num();
        // schedule dynamic may be faster but it won't be deterministic
        #pragma omp for schedule(static) collapse(2)
        for (int i = 0; i < board->size; i++) {
            for (int j = 0; j < board->size; j++) {
                if (boards[rank]->cells[i][j] == ' ') {
                    boards[rank]->cells[i][j] = symbol;
                    int score = minimax(boards[rank], 0, 0, LOWER_LIMIT, UPPER_LIMIT, maxDepth, color);
                    boards[rank]->cells[i][j] = ' ';
                    if (score > scoresMoves[rank]->bestScore) {
                        scoresMoves[rank]->bestScore = score;
                        scoresMoves[rank]->moveRow = i;
                        scoresMoves[rank]->moveCol = j;
                    }
                }
            }
        }

        #pragma omp single 
        {
            globalBestScore = LOWER_LIMIT;
            for (int i = 0; i < NUMTHREADS; i++) {
                if (scoresMoves[i]->bestScore > globalBestScore) {
                    globalBestScore = scoresMoves[i]->bestScore;
                    globalMoveRow = scoresMoves[i]->moveRow;
                    globalMoveCol = scoresMoves[i]->moveCol;
                }
            }
        }
    }

    for (int i = 0; i<NUMTHREADS; i++){
        free(scoresMoves[i]);
    }
    free(scoresMoves);

    board->cells[globalMoveRow][globalMoveCol] = symbol;
    (*totalMoves)++;
    for (int i = 0; i < NUMTHREADS; i++) {
        freeAllocatedBoard(boards[i]);
    }
}


void computerMoveParallelV2(Board *board, char symbol, int *totalMoves, int numThreads, int maxDepth){
    int color = (symbol == 'O') ? 1 : -1;

    Board *boards[NUMTHREADS];
    for (int i = 0; i < NUMTHREADS; i++) {
        boards[i] = boardClone(board);
    }

    int globalMoveRow = -1, globalMoveCol = -1;
    int globalBestScore = LOWER_LIMIT;

    ScoreMovePair_t **scoresMoves = (ScoreMovePair_t **)malloc(NUMTHREADS * sizeof(ScoreMovePair_t *));
    for (int i = 0; i < NUMTHREADS; i++) {
        scoresMoves[i] = (ScoreMovePair_t *)malloc(sizeof(ScoreMovePair_t));
        scoresMoves[i]->bestScore = LOWER_LIMIT;
        scoresMoves[i]->moveRow = -1;
        scoresMoves[i]->moveCol = -1;
    }

    #pragma omp parallel num_threads(numThreads)
    {
        #pragma omp single
        for (int i = 0; i < board->size; i++) {
            for (int j = 0; j < board->size; j++) {
                #pragma omp task shared(boards, scoresMoves, symbol, color, totalMoves) firstprivate(i, j)
                {
                    int rank = omp_get_thread_num();
                    if (boards[rank]->cells[i][j] == ' ') {
                        boards[rank]->cells[i][j] = symbol;
                        int score = minimax(boards[rank], 0, 0, LOWER_LIMIT, UPPER_LIMIT, maxDepth, color);
                        boards[rank]->cells[i][j] = ' ';
                        if (score > scoresMoves[rank]->bestScore) {
                            scoresMoves[rank]->bestScore = score;
                            scoresMoves[rank]->moveRow = i;
                            scoresMoves[rank]->moveCol = j;
                        }
                    }
                }
            }
        }
        #pragma omp taskwait
        #pragma omp single 
        {
            globalBestScore = LOWER_LIMIT;
            for (int i = 0; i < NUMTHREADS; i++) {
                if (scoresMoves[i]->bestScore > globalBestScore) {
                    globalBestScore = scoresMoves[i]->bestScore;
                    globalMoveRow = scoresMoves[i]->moveRow;
                    globalMoveCol = scoresMoves[i]->moveCol;
                }
            }
        }
    }

    for (int i = 0; i<NUMTHREADS; i++){
        free(scoresMoves[i]);
    }
    free(scoresMoves);

    board->cells[globalMoveRow][globalMoveCol] = symbol;
    (*totalMoves)++;
    for (int i = 0; i < NUMTHREADS; i++) {
        freeAllocatedBoard(boards[i]);
    }
}

void computerMoveParallel(Board *board, char symbol, int parallelVersion, int *totalMoves, int numThreads, int maxDepth){
    switch (parallelVersion){
        case 1:
            computerMoveParallelV1(board, symbol, totalMoves, numThreads, maxDepth);
            break;
        case 2:
            computerMoveParallelV2(board, symbol, totalMoves, numThreads, maxDepth);
            break;
        default:
            fprintf(stderr, "Invalid parallel version specified\n");
            exit(EXIT_FAILURE);
            break;
    }
    
}

void playHumanVsComputer(Board *board, int parallel, char *winner, int *moves, int parallelVersion, int numThreads, int maxDepth){
    struct timespec start, finish;
    double elapsed;
    while (1) {
        printBoard(board);
        playerMove(board, 'X');
        (*moves)++;
        if (checkWin(board, 'X')) {
            *winner = 'X';
            break;
        }
        if (isBoardFull(board)) {
            break;
        }

        clock_gettime(CLOCK_MONOTONIC, &start);
        if (parallel){
            computerMoveParallel(board, 'O', parallelVersion, moves, numThreads, maxDepth);
            (*moves)++;
        } else {
            computerMove(board, 'O', moves, maxDepth);
            (*moves)++;
        }
        clock_gettime(CLOCK_MONOTONIC, &finish);
        printBoard(board);
        elapsed = (finish.tv_sec - start.tv_sec);
        elapsed += (finish.tv_nsec - start.tv_nsec) / 1000000000.0;
        printf("Computer thought for %.5f seconds..\n", elapsed);

        if (checkWin(board, 'O')) {
            *winner = 'O';
            break;
        }
        if (isBoardFull(board)) {
            break;
        }
    }
}



void playComputerVsHuman(Board *board, int parallel, char *winner, int *moves, int parallelVersion, int numThreads, int maxDepth){
    struct timespec start, finish;
    double elapsed;
    while (1){
        clock_gettime(CLOCK_MONOTONIC, &start);
        if (parallel) {
            computerMoveParallel(board, 'X', parallelVersion, moves, numThreads, maxDepth);
            (*moves)++;
        } else {
            computerMove(board, 'X', moves, maxDepth);
            (*moves)++;
        }
        clock_gettime(CLOCK_MONOTONIC, &finish);
        printBoard(board);
        elapsed = (finish.tv_sec - start.tv_sec);
        elapsed += (finish.tv_nsec - start.tv_nsec) / 1000000000.0;
        printf("Computer thought for %.5f seconds..\n", elapsed);

        if (checkWin(board, 'X')) {
            *winner = 'X';
            break;
        }

        if (isBoardFull(board)) {
            break;
        }

        playerMove(board, 'O');
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
void playComputerVsComputer(Board *board, int parallel, char *winner, int parallelVersion, int numThreads, int maxDepth, FILE *outputFile){
    if (outputFile != NULL){
        fprintf(outputFile, "%d,%d,%d,%d,%d,", board->size, maxDepth, numThreads, parallel, parallelVersion);
    }
    struct timespec start, finish;
    double elapsed;
    clock_gettime(CLOCK_MONOTONIC, &start);
    int firstMove = 0;
    int totalMoves = 0;

    while(1){
        if (!firstMove){
            firstMove = 1;
            int firstMoveRow = board->size / 2;
            int firstMoveCol = board->size / 2;
            board->cells[firstMoveRow][firstMoveCol] = 'X';
        } else {
            if (parallel) {
                computerMoveParallel(board, 'X', parallelVersion, &totalMoves, numThreads, maxDepth);
            } else {
                computerMove(board, 'X', &totalMoves, maxDepth);
            }
        }
    #ifdef DEBUG
        printBoard(board);
    #endif

        if (checkWin(board, 'X')) {
            *winner = 'X';
            break;
        }

        if (isBoardFull(board)) {
            break;
        }

        if (parallel){
            computerMoveParallel(board, 'O', parallelVersion, &totalMoves, numThreads, maxDepth);
        } else {
            computerMove(board, 'O', &totalMoves, maxDepth);
        }
    #ifdef DEBUG
        printBoard(board);
    #endif

        if (checkWin(board, 'O')) {
            *winner = 'O';
            break;
        }
        if (isBoardFull(board)) {
            break;
        }
    }

    clock_gettime(CLOCK_MONOTONIC, &finish);
    elapsed = (finish.tv_sec - start.tv_sec);
    elapsed += (finish.tv_nsec - start.tv_nsec) / 1000000000.0;
    if (outputFile != NULL){
        fprintf(outputFile, "%.5f,%.5f\n", elapsed, elapsed / totalMoves);
    } else {
        printf("Elapsed time: %.5f seconds\n", elapsed);
        printf("Average time per move: %.5f seconds\n", elapsed / totalMoves);
    }
}