package com.github.anivanovic.jezik;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }

        return statements;
    }

    private Stmt declaration() {
      try{
        if (match(TokenType.VAR)) {
          return varStatement();
        } else if (match(TokenType.FUN)) {
          return funStatement("function");
        }
        return statement();
      } catch (ParseError e) {
          synchronize();
          return null;
      }
    }

  private Stmt.Function funStatement(String kind) {
    Token name = consume(TokenType.IDENTIFIER, "Expect " + kind + " name.");
    consume(TokenType.LEFT_PAREN, "Expect '(' after " + kind + " name ");
    List<Token> params = new ArrayList<>();
    do {
        if (params.size() == 255) {
            throw new RuntimeError(peek(), "Can not have more then 255 parameters");
        }
        if (check(TokenType.IDENTIFIER)) {
            params.add(consume(TokenType.IDENTIFIER, "Expect parameter name."));
        }
    } while (match(TokenType.COMMA));

    consume(TokenType.RIGHT_PAREN, "Expect ')' after " + kind + " parameters");
    consume(TokenType.LEFT_BRACE, "Expect '{' before " + kind + " body.");
    return new Stmt.Function(name, params, blockStatement().statements);
  }

  private Stmt statement() {
        if (match(TokenType.PRINT)) return printStatement();
        if (match(TokenType.LEFT_BRACE)) return blockStatement();
        if (match(TokenType.IF)) return ifStatement();
        if (match(TokenType.WHILE)) return whileStatement();
        if (match(TokenType.FOR)) return forStatement();
        if (match(TokenType.RETURN)) return returnStatement();

        return expressionStatement();
    }

    private Stmt.Return returnStatement() {
        Token token = previous();
        Expr value = null;
        if (!match(TokenType.SEMICOLON)) {
            value = expression();
        }
        consume(TokenType.SEMICOLON, "Expect ';' after return statement");
        return new Stmt.Return(token, value);
    }

    private Stmt forStatement() {
        consume(TokenType.LEFT_PAREN, "Expected '(' after for.");
        Stmt initializer;
        if (match(TokenType.SEMICOLON)) {
            initializer = null;
        } else if (match(TokenType.VAR)) {
            initializer = varStatement();
        } else {
            initializer = expressionStatement();
        }

        Expr condition = null;
        if (!check(TokenType.SEMICOLON)) {
            condition = expression();
        }
        consume(TokenType.SEMICOLON, "Expected ';' after loop condition.");

        Expr increment = null;
        if (!check(TokenType.RIGHT_PAREN)) {
            increment = expression();
        }
        consume(TokenType.RIGHT_PAREN, "Expected ')' for clauses.");
        Stmt body = statement();

        if (increment != null) {
            body = new Stmt.Block(List.of(body, new Stmt.Expression(increment)));
        }
        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        if (initializer != null) {
            body = new Stmt.Block(List.of(initializer, body));
        }

        return body;
    }

    private Stmt whileStatement() {
        consume(TokenType.LEFT_PAREN, "Expected '(' after while.");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expected ')' after while condition.");

        Stmt body = statement();
        return new Stmt.While(condition, body);
    }

    private Stmt ifStatement() {
        consume(TokenType.LEFT_PAREN, "Expected '(' after if.");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expected ')' after if condition.");

        Stmt thenBlock = statement();
        Stmt elseBlock = null;
        if (match(TokenType.ELSE)) {
            elseBlock = statement();
        }

        return new Stmt.If(condition, thenBlock, elseBlock);
    }

    private Stmt varStatement() {
        Token name = consume(TokenType.IDENTIFIER, "Expect variable name.");
        Expr initializer = null;
        if (match(TokenType.EQUAL)) {
            initializer = expression();
        }

        consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    private Stmt.Block blockStatement() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(TokenType.RIGHT_BRACE, "Expected '}' after block.");
        return new Stmt.Block(statements);
    }

    private Stmt printStatement() {
        Stmt.Print print = new Stmt.Print(expression());
        consume(TokenType.SEMICOLON, "Expect ';' after expression.");
        return print;
    }

    private Stmt expressionStatement() {
        Stmt.Expression exprStmt = new Stmt.Expression(expression());
        consume(TokenType.SEMICOLON, "Expect ';' after expression.");
        return exprStmt;
    }

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr expr = or();

        if (match(TokenType.EQUAL)) {
            Token equal = previous();
            Expr val = assignment();

            if (expr instanceof Expr.Variable) {
                return new Expr.Assign(((Expr.Variable) expr).name, val);
            }

            error(equal, "Invalid assignment target.");
        }

        return expr;
    }

    private Expr or() {
        Expr expr = and();

        while (match(TokenType.OR)) {
            Token operator = previous();
            Expr right = and();
            return new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr and() {
        Expr expr = equality();

        while (match(TokenType.AND)) {
            Token operator = previous();
            Expr right = equality();
            return new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();
        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr comparison() {
        Expr expr = term();
        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term() {
        Expr expr = factory();
        while (match(TokenType.MINUS, TokenType.PLUS)) {
            Token operator = previous();
            Expr right = factory();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factory() {
        Expr expr = unary();
        while (match(TokenType.SLASH, TokenType.STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return call();
    }

    private Expr call() {
        Expr expr = primary();
        while(true) {
            if (!match(TokenType.LEFT_PAREN)) {
                break;
            }
            expr = finishCall(expr);
        }
        return expr;
    }

    private Expr finishCall(Expr expr) {
        List<Expr> args = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (args.size() >= 255) {
                    error(peek(), "Not allowed more then 255 arguments.");
                }
                args.add(expression());
            } while (match(TokenType.COMMA));
        }

        Token paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after argument list.");
        return new Expr.Call(expr, null, args);
    }

    private Expr primary() {
        if (match(TokenType.FALSE)) return new Expr.Literal(false);
        if (match(TokenType.TRUE)) return new Expr.Literal(true);
        if (match(TokenType.NIL)) return new Expr.Literal(null);

        if (match(TokenType.NUMBER, TokenType.STRING)) return new Expr.Literal(previous().literal);

        if (match(TokenType.LEFT_PAREN)) {
            Expr expr = expression();
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        if (match(TokenType.IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        throw error(peek(), "Expect expression.");
    }

    private Token consume(TokenType token, String errMsg) {
        if (check(token)) return advance();
        throw error(peek(), errMsg);
    }

    private ParseError error(Token token, String errMsg) {
        Lox.error(token, errMsg);
        return new ParseError();
    }

    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON) return;

            switch (peek().type) {
                case CLASS: case FOR: case FUN: case IF:
                case PRINT: case RETURN: case VAR: case WHILE:
                    return;
            }
        }
    }

    private boolean match(TokenType... tokens) {
        for (TokenType token : tokens) {
            if (check(token)) {
                advance();
                return true;
            }
        }

        return false;
    }

    private boolean check(TokenType token) {
        if (isAtEnd()) return false;
        return peek().type == token;
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }
}
