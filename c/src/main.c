#include "common.h"
#include "chunk.h"
#include "debug.h"
#include "vm.h"

int main(int argc, const char* argv[]) {
  initVM();


  Chunk chunk;
  initChunk(&chunk);

  int index = addConstant(&chunk, 1.2);
  writeChunk(&chunk, OP_CONST, 123);
  writeChunk(&chunk, index, 123);

  index = addConstant(&chunk, 3.4);
  writeChunk(&chunk, OP_CONST, 123);
  writeChunk(&chunk, index, 123);

  writeChunk(&chunk, OP_ADD, 123);

  index = addConstant(&chunk, 5.6);
  writeChunk(&chunk, OP_CONST, 123);
  writeChunk(&chunk, index, 123);

  writeChunk(&chunk, OP_DIVIDE, 123);

  writeChunk(&chunk, OP_NEGATE, 123);
  writeChunk(&chunk, OP_RETURN, 123);

  disassembleChunk(&chunk, "test chunk");
  interpret(&chunk);

  freeVM();
  freeChunk(&chunk);

  return EXIT_SUCCESS;
}