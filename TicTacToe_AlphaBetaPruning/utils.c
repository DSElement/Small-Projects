#include "utils.h"

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

                    int score = -negamax(boards[rank], 1, -beta, -alpha, MAXDEPTH, -color);

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

void playHumanVsComputer(Board *board, int parallel, char *winner, int *moves){
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
void playComputerVsComputer(Board *board, int parallel, char *winner){
    struct timespec start, finish;
    double elapsed;
    clock_gettime(CLOCK_MONOTONIC, &start);

    while(1){
        if (parallel) {
            computerMoveParallel(board, 'X');
        } else {
            computerMove(board, 'X');
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
            computerMoveParallel(board, 'O');
        } else {
            computerMove(board, 'O');
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
    printf("Elapsed time: %.5f seconds\n", elapsed);
}