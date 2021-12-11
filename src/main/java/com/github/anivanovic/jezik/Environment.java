package com.github.anivanovic.jezik;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    private final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();

    public Environment() {
        enclosing = null;
    }

    public Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    public void define(Token name, Object value){
        if (!values.containsKey(name.lexem)) {
            values.put(name.lexem, value);
            return;
        }

        throw new RuntimeError(name, "Variable '" + name.lexem + "' already declared.");
    }

    public Object get(Token name) {
        if (values.containsKey(name.lexem)) return values.get(name.lexem);
        if (enclosing != null) return enclosing.get(name);

        throw new RuntimeError(name, "Undefined variable '" + name.lexem + "'.");
    }

    public void assign(Token name, Object val) {
        if (values.containsKey(name.lexem)) {
            values.put(name.lexem, val);
            return;
        }
        if (enclosing != null) {
          enclosing.assign(name, val);
          return;
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexem + "'.");
    }

    public Object getAt(Token name, Integer dist) {
        return ancestor(dist).values.get(name);
    }

    public void assignAt(Integer dist, Token name, Object val) {
        ancestor(dist).values.put(name.lexem, val);
    }

    private Environment ancestor(Integer dist) {
        Environment env = this;
        for (int i = 0; i < dist; i++) {
            env = env.enclosing;
        }

        return env;
    }
}
