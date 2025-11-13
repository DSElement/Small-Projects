#include "utils.h"


int main() {

    Board board;
    printf("Enter the size of the board (N): ");

    while (1) {
        scanf("%d", &board.size);
        if (board.size < BOARDMINSIZE || board.size > BOARDMAXSIZE) {
            printf("Invalid size. Please enter a number between %d and %d: ", BOARDMINSIZE, BOARDMAXSIZE);
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

        #ifndef DEBUG
            printBoard(&board);
        #endif

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
