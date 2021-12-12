#ifndef __PROGRAMMING_JEZIK_CLOX_SRC_CHUNK_H_
#define __PROGRAMMING_JEZIK_CLOX_SRC_CHUNK_H_

#include "common.h"
#include "value.h"

typedef enum {
  OP_CONST,
  OP_ADD,
  OP_SUBTRACT,
  OP_MULTIPLY,
  OP_DIVIDE,
  OP_NEGATE,
  OP_RETURN,
} OpCode;

typedef struct {
  int count;
  int capacity;
  u_int8_t* code;
  ValueArray constants;
  int* lines;
} Chunk;

void initChunk(Chunk* chunk);
void freeChunk(Chunk* chunk);
void writeChunk(Chunk* chunk, u_int8_t code, int line);
int addConstant(Chunk* chunk, Value value);

#endif // __PROGRAMMING_JEZIK_CLOX_SRC_CHUNK_H_