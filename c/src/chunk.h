#ifndef __PROGRAMMING_JEZIK_CLOX_SRC_CHUNK_H_
#define __PROGRAMMING_JEZIK_CLOX_SRC_CHUNK_H_

#include "common.h"

typedef enum {
  OP_RETURN,
} OpCode;

typedef struct {
  int count;
  int capacity;
  u_int8_t* code;
} Chunk;

void initChunk(Chunk* chunk);
void freeChunk(Chunk* chunk);
void writeChunk(Chunk* chunk, u_int8_t code);

#endif // __PROGRAMMING_JEZIK_CLOX_SRC_CHUNK_H_