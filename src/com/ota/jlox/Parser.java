package com.ota.jlox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Parser {
	private static class ParseError extends RuntimeException {}

	private final List<Token> tokens;
	private int current = 0;

	Parser(List<Token> tokens) {
		this.tokens = tokens;
	}

	public List<Stmt> parse() {
		List<Stmt> statements = new ArrayList<>();
		while(!isAtEnd()) {
			statements.add(declaration());
		}
		return statements;
	}

	private Stmt declaration() {
		try {
			if(match(TokenType.FUN)) return function("function");
			if(match(TokenType.VAR)) return varDeclaration();
			return statement();
		} catch(ParseError error) {
			syncronize();
			return null;
		}
	}

	private Stmt varDeclaration() {
		Token name = consume(TokenType.IDENTIFIER, "Expect variable name.");

		Expr initializer = null;
		if(match(TokenType.EQUAL)) {
			initializer = expression();
		}

		consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.");
		return new Stmt.Var(name, initializer);
	}

	private Stmt.Function function(String kind) {
		Token name = consume(TokenType.IDENTIFIER, "Expect " + kind + " name.");
		consume(TokenType.LEFT_PAREN, "Expect '(' after " + kind + " name.");
		List<Token> parameters = new ArrayList<>();
		if(!check(TokenType.RIGHT_PAREN)) {
			do {
				if(parameters.size() >= 255) {
					error(peek(), "Can't habe more than 255 parameters.");
				}

				parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name."));
			} while(match(TokenType.COMMA));
		}
		consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.");

		consume(TokenType.LEFT_BRACE, "Expect '{' before " + kind + " body.");
		List<Stmt> body = block();
		return new Stmt.Function(name, parameters, body);
	}

	private Stmt statement() {
		if(match(TokenType.IF)) return ifStatement();
		if(match(TokenType.FOR)) return forStatement();
		if(match(TokenType.WHILE)) return whileStatement();
		if(match(TokenType.PRINT)) return printStatement();
		if(match(TokenType.RETURN)) return returnStatement();
		if(match(TokenType.LEFT_BRACE)) return new Stmt.Block(block());
		return expressionStatement();
	}

	private Stmt ifStatement() {
		consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.");
		Expr condition = expression();
		consume(TokenType.RIGHT_PAREN, "Expect ')' after 'if' condition.");

		Stmt thenBranch = statement();
		Stmt elseBranch = null;
		if(match(TokenType.ELSE))
			elseBranch = statement();

		return new Stmt.If(condition, thenBranch, elseBranch);
	}

	private Stmt forStatement() {
		consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'.");

		Stmt initializer;
		if(match(TokenType.SEMICOLON)) {
			initializer = null;
		} else if(match(TokenType.VAR)) {
			initializer = varDeclaration();
		} else {
			initializer = expressionStatement();
		}

		Expr condition = null;
		if(!check(TokenType.SEMICOLON)) {
			condition = expression();
		}
		consume(TokenType.SEMICOLON, "Expect ';' after loop condition.");

		Expr increment = null;
		if(!check(TokenType.RIGHT_PAREN)) {
			increment = expression();
		}
		consume(TokenType.RIGHT_PAREN, "Expect ')' after for clauses.");

		Stmt body = statement();

		if(increment != null) {
			body = new Stmt.Block(
				Arrays.asList(
					body,
					new Stmt.Expression(increment)
				)
			);
		}

		if(condition == null) condition = new Expr.Literal(true);
		body = new Stmt.While(condition, body);

		if(initializer != null) {
			body = new Stmt.Block(
				Arrays.asList(
					initializer,
					body
				)
			);
		}

		return body;
	}

	private Stmt whileStatement() {
		consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.");
		Expr condition = expression();
		consume(TokenType.RIGHT_PAREN, "Expect ')' after condition.");
		Stmt body = statement();

		return new Stmt.While(condition, body);
	}

	private Stmt printStatement() {
		Expr value = expression();
		consume(TokenType.SEMICOLON, "Expect ';' after value.");
		return new Stmt.Print(value);
	}

	private Stmt returnStatement() {
		Token keyword = previous();
		Expr value = null;
		if(!check(TokenType.SEMICOLON)) value = expression();
		consume(TokenType.SEMICOLON, "Expect ';' after return value.");
		return new Stmt.Return(keyword, value);
	}

	private Stmt expressionStatement() {
		Expr expression = expression();
		consume(TokenType.SEMICOLON, "Expect ';' after expression.");
		return new Stmt.Expression(expression);
	}

	private List<Stmt> block() {
		List<Stmt> statements = new ArrayList<>();
		while(!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
			statements.add(declaration());
		}
		consume(TokenType.RIGHT_BRACE, "Expect '}' after block.");
		return statements;
	}

	private Expr expression() {
		return assignment();
	}

	private Expr assignment() {
		Expr expression = or();

		if(match(TokenType.EQUAL)) {
			Token equals = previous();
			Expr value = assignment();

			if(expression instanceof Expr.Variable) {
				Token name = ((Expr.Variable)expression).name;
				return new Expr.Assign(name, value);
			}

			error(equals, "Invalid assignment target.");
		}

		return expression;
	}

	private Expr or() {
		Expr expression = and();

		if(match(TokenType.OR)) {
			Token operator = previous();
			Expr right = and();
			expression = new Expr.Logical(expression, operator, right);
		}

		return expression;
	}

	private Expr and() {
		Expr expression= equality();

		if(match(TokenType.AND)) {
			Token operator = previous();
			Expr right = equality();
			expression = new Expr.Logical(expression, operator, right);
		}

		return expression;
	}

	private Expr equality() {
		Expr expression = comparison();

		while(match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
			Token operator = previous();
			Expr right = comparison();
			expression = new Expr.Binary(expression, operator, right);
		}

		return expression;
	}

	private Expr comparison() {
		Expr expression = term();

		while(match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
			Token operator = previous();
			Expr right = term();
			expression = new Expr.Binary(expression, operator, right);
		}

		return expression;
	}

	private Expr term() {
		Expr expression = factor();

		while(match(TokenType.MINUS, TokenType.PLUS)) {
			Token operator = previous();
			Expr right = factor();
			expression = new Expr.Binary(expression, operator, right);
		}

		return expression;
	}

	private Expr factor() {
		Expr expression = unary();

		while(match(TokenType.SLASH, TokenType.STAR)) {
			Token operator = previous();
			Expr right = unary();
			expression = new Expr.Binary(expression, operator, right);
		}

		return expression;
	}

	private Expr unary() {
		if(match(TokenType.BANG, TokenType.MINUS)) {
			Token operator = previous();
			Expr right = unary();
			return new Expr.Unary(operator, right);
		}

		return call();
	}

	private Expr call() {
		Expr expression = primary();

		while(true) {
			if(match(TokenType.LEFT_PAREN)) {
				expression = finishCall(expression);
			} else break;
		}

		return expression;
	}

	private Expr finishCall(Expr callee) {
		List<Expr> arguments = new ArrayList<Expr>();
		if(!check(TokenType.RIGHT_PAREN)) {
			do {
				if(arguments.size() >= 255) {
					error(peek(), "Can't have more than 255 arguments.");
				}
				arguments.add(expression());
			} while(match(TokenType.COMMA));
		}

		Token paren = consume(TokenType.RIGHT_PAREN, "Expect ')' at the end of function call.");
		return new Expr.Call(callee, paren, arguments);
	}

	private Expr primary() {
		if(match(TokenType.FALSE)) return new Expr.Literal(false);
		if(match(TokenType.TRUE)) return new Expr.Literal(true);
		if(match(TokenType.NIL)) return new Expr.Literal(null);

		if(match(TokenType.NUMBER, TokenType.STRING)) return new Expr.Literal(previous().literal);

		if(match(TokenType.IDENTIFIER)) return new Expr.Variable(previous());

		if(match(TokenType.LEFT_PAREN)) {
			Expr expression = expression();
			consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
			return new Expr.Grouping(expression);
		}

		throw error(peek(), "Expect expression.");
	}

	private boolean match(TokenType... types) {
		for(TokenType type : types) {
			if(check(type)) {
				advance();
				return true;
			}
		}

		return false;
	}

	private boolean check(TokenType type) {
		if(isAtEnd()) return false;
		return peek().type == type;
	}

	private Token advance() {
		if(!isAtEnd()) this.current++;
		return previous();
	}

	private Token consume(TokenType type, String message) {
		if(check(type)) return advance();

		throw error(peek(), message);
	}

	private boolean isAtEnd() {
		return peek().type == TokenType.EOF;
	}

	private Token peek() {
		return this.tokens.get(this.current);
	}

	private Token previous() {
		return this.tokens.get(this.current - 1);
	}

	private ParseError error(Token token, String message) {
		Main.error(token, message);
		return new ParseError();
	}

	private void syncronize() {
		advance();

		while(!isAtEnd()) {
			if(previous().type == TokenType.SEMICOLON) return;

			switch(peek().type) {
				case CLASS:
				case FUN:
				case VAR:
				case FOR:
				case IF:
				case WHILE:
				case PRINT:
				case RETURN:
					return;
				default:
					break;
			}

			advance();
		}
	}

}