#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <ctype.h>
#include <omp.h>

#define DEBUG 1
#define LOWER_LIMIT -1000
#define UPPER_LIMIT 1000
#define NUMTHREADS 8
#define MAXDEPTH 7
#define BOARDMINSIZE 2
#define BOARDMAXSIZE 15
#define PARALLELVERSION 1


typedef struct {
    int size;
    char **cells;
} Board;

void initializeBoard(Board *board);
void printBoard(Board *board);
void playerMove(Board *board, char symbol);
void generateMoveOrder(int size, int moves[][2], int *count);
void computerMove(Board *board, char symbol);
void computerMoveParallel(Board *board, char symbol, int parallelVersion);
void playHumanVsComputer(Board *board, int parallel, char *winner, int *moves);
void playComputerVsHuman(Board *board, int parallel, char *winner, int *moves);
void playComputerVsComputer(Board *board, int parallel, char *winner);