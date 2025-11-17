#include "utils.h"


int main() {
/*
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
            playHumanVsComputer(&board, parallelChoice, &winner, &moves, PARALLELVERSION, NUMTHREADS, MAXDEPTH);
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
            playComputerVsHuman(&board, parallelChoice, &winner, &moves, PARALLELVERSION, NUMTHREADS, MAXDEPTH);
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
            playComputerVsComputer(&board, parallelChoice, &winner, PARALLELVERSION, NUMTHREADS, MAXDEPTH, NULL);

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
*/


    //Performance measurements: For different sizes of the board (N>4) and different depths of the 
    //minimax search algorithm, for different number of threads (2, 4, 8), with and without parallelism:

    FILE *outputFile = fopen("results.csv", "w");
    if (!outputFile) {
        fprintf(stderr, "Error opening file for writing results.\n");
        return EXIT_FAILURE;
    }
    fprintf(outputFile, "BoardSize,MaxDepth,ThreadCount,WithParallelism,ParallelVersion,RunTime,AverageTimePerMove\n");

    Board board;

    int boardSizes[] = {3,4,5,6,7,8};
    int boardSizesLength = sizeof(boardSizes) / sizeof(boardSizes[0]);
    int maxDepths[] = {5, 6, 7, 9, 11};
    int maxDepthsLength = sizeof(maxDepths) / sizeof(maxDepths[0]);
    int threadCounts[] = {2, 4, 8};
    int threadCountsLength = sizeof(threadCounts) / sizeof(threadCounts[0]);
    int parallelVersions[] = {1, 2};
    int parallelVersionsLength = sizeof(parallelVersions) / sizeof(parallelVersions[0]);

    char winner;

    for (int bi = 0; bi < boardSizesLength; bi++)
    {
        board.size = boardSizes[bi];

        board.cells = (char **)malloc(board.size * sizeof(char *));
        for (int r = 0; r < board.size; r++)
        {
            board.cells[r] = (char *)malloc(board.size * sizeof(char));
        }

        for (int dj = 0; dj < maxDepthsLength; dj++)
        {
            int maxDepth = maxDepths[dj];

            if (board.size > 6 && maxDepth > 7 )
            {
                continue;
            }

            initializeBoard(&board);
            playComputerVsComputer(&board, 0, &winner, 0, 1, maxDepth, outputFile);

            for (int tk = 0; tk < threadCountsLength; tk++)
            {
                int numThreads = threadCounts[tk];

                for (int pv = 0; pv < parallelVersionsLength; pv++)
                {
                    int parallelVersion = parallelVersions[pv];

                    initializeBoard(&board);
                    playComputerVsComputer(&board, 1, &winner, parallelVersion, numThreads, maxDepth, outputFile);
                }
            }
        }

        for (int r = 0; r < board.size; r++)
        {
            free(board.cells[r]);
        }
        free(board.cells);
    }

    fclose(outputFile);

    return 0;
}
