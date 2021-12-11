package com.github.anivanovic.jezik;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
  Environment globals = new Environment();
  private Environment environment = globals;
  private final Map<Expr, Integer> locals = new HashMap<>();

  {
    globals.define(new Token(null, "clock", "clock", 0), new LoxCallable() {
      @Override
      public int arity() {
        return 0;
      }

      @Override
      public Object call(Interpreter interpreter, List<Object> args) {
        return ((double)System.currentTimeMillis() / 1000.0);
      }

      @Override
      public String toString() {
        return "<native fn>";
      }
    });
  }

  void interpret(List<Stmt> statements) {
    try {
      for (Stmt statement : statements) {
        execute(statement);
      }
    } catch (RuntimeError e) {
      Lox.runtimeError(e);
    }
  }

  private void execute(Stmt statement) {
    statement.accept(this);
  }

  private String stringify(Object val) {
    if (val == null) {
      return "nil";
    }

    if (val instanceof Double) {
      String text = val.toString();
      if (text.endsWith(".0")) {
        text = text.substring(0, text.length() - 2);
      }
      return text;
    }

    return val.toString();
  }

  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case PLUS:
        if (left instanceof Double && right instanceof Double) {
          return (double) left + (double) right;
        }
        if (left instanceof String && right instanceof String) {
          return left + (String) right;
        }

        throw new RuntimeError(expr.operator, "Operands must be numbers or strings.");
      case MINUS:
        checkNumberOperand(expr.operator, left, right);
        return (double) left - (double) right;
      case STAR:
        checkNumberOperand(expr.operator, left, right);
        return (double) left * (double) right;
      case SLASH:
        checkNumberOperand(expr.operator, left, right);
        return (double) left / (double) right;
      case GREATER:
        checkNumberOperand(expr.operator, left, right);
        return (double) left > (double) right;
      case GREATER_EQUAL:
        checkNumberOperand(expr.operator, left, right);
        return (double) left >= (double) right;
      case LESS:
        checkNumberOperand(expr.operator, left, right);
        return (double) left < (double) right;
      case LESS_EQUAL:
        checkNumberOperand(expr.operator, left, right);
        return (double) left <= (double) right;
      case EQUAL_EQUAL:
        checkNumberOperand(expr.operator, left, right);
        return isEqual(left, right);
      case BANG_EQUAL:
        checkNumberOperand(expr.operator, left, right);
        return !isEqual(left, right);
    }

    // UNREACHABLE
    return null;
  }

  private boolean isEqual(Object left, Object right) {
    if (left == null && right == null) return true;
    if (left == null) return false;
    return left.equals(right);
  }

  @Override
  public Object visitGroupingExpr(Expr.Grouping expr) {
    return evaluate(expr.expression);
  }

  @Override
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
  }

  @Override
  public Object visitUnaryExpr(Expr.Unary expr) {
    Object val = evaluate(expr.right);
    switch (expr.operator.type) {
      case MINUS:
        checkNumberOperand(expr.operator, val);
        return -(double) val;
      case BANG:
        return !isTruthy(val);
    }

    // UNREACHABLE
    return null;
  }

  @Override
  public Object visitLogicalExpr(Expr.Logical expr) {
    Object val = evaluate(expr.left);
    if (expr.operator.type == TokenType.OR) {
      if (isTruthy(val)) return val;
    } else {
      if (!isTruthy(val)) return val;
    }

    return evaluate(expr.right);
  }

  private void checkNumberOperand(Token operator, Object val) {
    if (val instanceof Double) return;
    throw new RuntimeError(operator, "Operand must be a number.");
  }

  private void checkNumberOperand(Token operator, Object left, Object right) {
    if (left instanceof Double && right instanceof Double) return;
    throw new RuntimeError(operator, "Operands must be a numbers.");
  }

  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }

  private boolean isTruthy(Object val) {
    if (val == null) return false;
    if (val instanceof Boolean) return (boolean) val;

    return true;
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    evaluate(stmt.expression);
    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    Object val = evaluate(stmt.expression);
    System.out.println(stringify(val));
    return null;
  }

  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    Object val = null;
    if (stmt.initializer != null) {
      val = evaluate(stmt.initializer);
    }
    environment.define(stmt.name, val);
    return null;
  }

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    executeBlock(stmt.statements, new Environment(environment));
    return null;
  }

  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    Object val = stmt.condition.accept(this);
    if (isTruthy(val)) {
      execute(stmt.thenBranch);
    } else if (stmt.elseBranch != null) {
      execute(stmt.elseBranch);
    }
    return null;
  }

  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    while (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.body);
    }

    return null;
  }

  void executeBlock(List<Stmt> statements, Environment environment) {
    Environment previous = this.environment;
    try {
      this.environment = environment;
      for (Stmt statement : statements) {
        statement.accept(this);
      }

    } finally {
      this.environment = previous;
    }
  }

  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    return lookUpVar(expr.name, expr);
  }

  @Override
  public Object visitAssignExpr(Expr.Assign expr) {
    Object val = evaluate(expr.value);
    Integer dist = locals.get(expr);
    if (dist != null) {
      environment.assignAt(dist, expr.name, val);
    } else {
      return globals.get(expr.name);
    }
    return val;
  }

  @Override
  public Object visitCallExpr(Expr.Call expr) {
    Object fn = evaluate(expr.callee);

    List<Object> args = new ArrayList<>();
    for (Expr arg : expr.args) {
      args.add(evaluate(arg));
    }

    if (!(fn instanceof LoxCallable)) {
      throw new RuntimeError(expr.paren, "Can only call functions and classes.");
    }

    LoxCallable fnCallable = (LoxCallable) fn;
    if (fnCallable.arity() != args.size()) {
      throw new RuntimeError(
          expr.paren, "Expected " + fnCallable.arity() + " arguments but got " + args.size() + ".");
    }
    return fnCallable.call(this, args);
  }

  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    LoxFunction function = new LoxFunction(stmt, environment);
    environment.define(stmt.name, function);
    return null;
  }

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    if (stmt.value == null) return null;
    throw new Return(evaluate(stmt.value));
  }

  public void resolve(Expr expr, int i) {
    locals.put(expr, i);
  }

  private Object lookUpVar(Token name, Expr.Variable expr) {
    Integer dist = locals.get(expr);
    if (dist != null) {
      return environment.getAt(name, dist);
    } else {
      return globals.get(name);
    }
  }
}
