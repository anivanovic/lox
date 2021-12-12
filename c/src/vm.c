#include <stdio.h>

// #include "value.h"
#include "common.h"
#include "vm.h"

VM vm;

void initVM() {

}

void freeVM() {

}

static InterpretResult run() {
    #define READ_BYTE() (*vm.ip++)
    #define READ_CONST() (vm.chunk->constants.values[READ_BYTE()])

    uint8_t instruction;
    for (;;) {
        instruction = READ_BYTE();
        switch (instruction) {
            case OP_RETURN:
                return INTERPRET_OK;
            case OP_CONST: {
                Value constant = READ_CONST();
                printValue(constant);
                printf("\n");
                break;
            }
        }
    }
    

    #undef READ_BYTE
    #undef READ_CONST
}

InterpretResult interpret(Chunk* chunk) {
    vm.chunk = chunk;
    vm.ip = chunk->code;
    return run();
}