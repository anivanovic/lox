package com.github.anivanovic.jezik;

import java.util.*;

public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {

  enum FunctionType {
    NONE,
    FUNCTION,
  }

  private final Interpreter interpreter;
  private final Stack<Map<String, Boolean>> scope = new Stack<>();
  private FunctionType currentFunction = FunctionType.NONE;

  public Resolver(Interpreter interpreter) {
    this.interpreter = interpreter;
  }

  void resolve(List<Stmt> statements) {
    for (Stmt stmt : statements) {
      resolve(stmt);
    }
  }

  private void resolve(Stmt stmt) {
    stmt.accept(this);
  }

  private void resolve(Expr expr) {
    expr.accept(this);
  }

  private void resolveFunction(Stmt.Function stmt, FunctionType fType) {
    FunctionType tmp = currentFunction;
    currentFunction = fType;

    beginScope();
    for (Token parameter : stmt.variables) {
      declare(parameter);
    }
    resolve(stmt.body);
    endScope();

    currentFunction = tmp;
  }

  @Override
  public Void visitBinaryExpr(Expr.Binary expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitGroupingExpr(Expr.Grouping expr) {
    resolve(expr.expression);
    return null;
  }

  @Override
  public Void visitLiteralExpr(Expr.Literal expr) {
    return null;
  }

  @Override
  public Void visitUnaryExpr(Expr.Unary expr) {
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitLogicalExpr(Expr.Logical expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitVariableExpr(Expr.Variable expr) {
    if (!scope.isEmpty() &&
          scope.peek().get(expr.name.lexem) == false) {
      Lox.error(expr.name, "Can not use variable in its own initializer.");
    }

    resolveLocal(expr, expr.name);
    return null;
  }

  @Override
  public Void visitAssignExpr(Expr.Assign expr) {
    resolve(expr.value);
    resolveLocal(expr, expr.name);
    return null;
  }

  @Override
  public Void visitCallExpr(Expr.Call expr) {
    resolve(expr.callee);
    for (Expr arg : expr.args) {
      resolve(arg);
    }
    return null;
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    resolve(stmt.expression);
    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    resolve(stmt.expression);
    return null;
  }

  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    declare(stmt.name);
    if (stmt.initializer != null) {
      resolve(stmt.initializer);
    }
    define(stmt.name);
    return null;
  }

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    beginScope();
    resolve(stmt.statements);
    endScope();
    return null;
  }

  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    resolve(stmt.condition);
    resolve(stmt.thenBranch);
    if (stmt.elseBranch != null)
      resolve(stmt.elseBranch);
    return null;
  }

  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    resolve(stmt.condition);
    resolve(stmt.body);
    return null;
  }

  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    declare(stmt.name);
    define(stmt.name);
    resolveFunction(stmt, FunctionType.FUNCTION);
    return null;
  }

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    if (currentFunction == FunctionType.NONE) {
      Lox.error(stmt.token, "No function to return from");
    }
    if (stmt.value != null)
      resolve(stmt.value);
    return null;
  }

  private void beginScope() {
    scope.push(new HashMap<>());
  }

  private void endScope() {
    scope.pop();
  }

  private void declare(Token name) {
    if (scope.isEmpty()) return;
    if (scope.peek().containsKey(name.lexem)) {
      Lox.error(name, "Variable with same name already in scope.");
    }
    scope.peek().put(name.lexem, false);
  }

  private void define(Token name) {
    if (scope.isEmpty()) return;
    scope.peek().put(name.lexem, true);
  }

  private void resolveLocal(Expr expr, Token name) {
    for (int i = scope.size() - 1; i >= 0; i--) {
      if (scope.get(i).containsKey(name.lexem)) {
        interpreter.resolve(expr, scope.size() - 1 - i);
        return;
      }
    }
  }
}
