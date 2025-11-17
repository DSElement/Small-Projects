#ifndef UTILS_H
#define UTILS_H

#include <stdio.h>
#include <stdlib.h>
#include <ctype.h>
#include <omp.h>

//#define DEBUG 1
#define LOWER_LIMIT -1000
#define UPPER_LIMIT 1000
#define NUMTHREADS 8
#define MAXDEPTH 25
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
void computerMove(Board *board, char symbol, int *totalMoves, int maxDepth);
void computerMoveParallel(Board *board, char symbol, int parallelVersion, int *totalMoves, int numThreads, int maxDepth);
void playHumanVsComputer(Board *board, int parallel, char *winner, int *moves, int parallelVersion, int numThreads, int maxDepth);
void playComputerVsHuman(Board *board, int parallel, char *winner, int *moves, int parallelVersion, int numThreads, int maxDepth);
void playComputerVsComputer(Board *board, int parallel, char *winner, int parallelVersion, int numThreads, int maxDepth, FILE *outputFile);

#endif // UTILS_H