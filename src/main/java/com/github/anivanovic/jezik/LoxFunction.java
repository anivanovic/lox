package com.github.anivanovic.jezik;

import java.util.List;

public class LoxFunction implements LoxCallable {

    private final Stmt.Function function;
    private final Environment closure;

    public LoxFunction(Stmt.Function function, Environment environment) {
        this.function = function;
        this.closure = environment;
    }

    @Override
    public int arity() {
        return function.variables.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> args) {
        Environment environment = new Environment(closure);

        for (int i = 0; i < args.size(); i++) {
            environment.define(function.variables.get(i), args.get(i));
        }
        try {
            interpreter.executeBlock(function.body, environment);
        } catch (Return e) {
            return e.value;
        }
        return null;
    }

    @Override
    public String toString() {
        return "<fn " + function.name.lexem + ">";
    }
}
