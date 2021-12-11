#include "common.h"
#include "chunk.h"
#include "debug.h"

int main(int argc, const char* argv[]) {
  Chunk chunk;
  initChunk(&chunk);

  int index = addConstant(&chunk, 1.2);
  writeChunk(&chunk, OP_CONST, 123);
  writeChunk(&chunk, index, 123);

  writeChunk(&chunk, OP_RETURN, 123);

  disassembleChunk(&chunk, "test chunk");

  freeChunk(&chunk);

  return EXIT_SUCCESS;
}