#ifndef __DEBUG_H__
#define __DEBUG_H__

#include "chunk.h"

void disassembleChunk(Chunk* chunk, const char* name);
int disassebleInstruction(Chunk* chunk, int offset);

#endif // __DEBUG_H__