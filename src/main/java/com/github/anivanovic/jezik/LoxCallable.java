package com.github.anivanovic.jezik;

import java.util.List;

public interface LoxCallable {

    int arity();
    Object call(Interpreter interpreter, List<Object> args);
}
