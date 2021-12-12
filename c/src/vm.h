#ifndef __VM_H__
#define __VM_H__

#include "chunk.h"

typedef struct {
    Chunk* chunk;
    uint8_t* ip;
} VM;

typedef enum
{
    INTERPRET_OK,
    INTERPRET_COMPILE_ERROR,
    INTERPRET_RUNTIME_ERROR,
} InterpretResult;

void initVM();
void freeVM();
InterpretResult interpret(Chunk* chunk);


#endif // __VM_H__