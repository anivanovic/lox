BUILD_DIR := build
SOURCE_DIR := src
NAME := clox


CFLAGS := -std=c99 -Wall -Wextra -Werror -Wno-unused-parameter -Wno-unused-function 

HEADERS := $(wildcard $(SOURCE_DIR)/*.h)
SOURCES := $(wildcard $(SOURCE_DIR)/*.c)
OBJECTS := $(addprefix $(BUILD_DIR)/, $(notdir $(SOURCES:.c=.o)))

# Targets ---------------------------------------------------------------------

clox: $(BUILD_DIR)/$(NAME)
	@ cp $(BUILD_DIR)/$(NAME) $(NAME)

# Link the interpreter.
$(BUILD_DIR)/$(NAME): $(OBJECTS)
	@ printf "%8s %-40s %s\n" $(CC) $@ "$(CFLAGS)"
	@ $(CC) $(CFLAGS) $^ -o $@

# Compile object files.
$(BUILD_DIR)/%.o: $(SOURCE_DIR)/%.c $(HEADERS)
	@ printf "%8s %-40s %s\n" $(CC) $< "$(CFLAGS)"
	@ mkdir -p $(BUILD_DIR)
	@ $(CC) -c $(C_LANG) $(CFLAGS) -o $@ $<


.PHONY: default clox