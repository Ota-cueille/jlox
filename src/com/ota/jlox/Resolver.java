package com.ota.jlox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
	private final Interpreter interpreter;
	private final Stack<Map<String, Boolean>> scopes = new Stack<>();
	private FunctionType currentFunction = FunctionType.NONE;

	Resolver(Interpreter interpreter) {
		this.interpreter = interpreter;
	}

	private enum FunctionType {
		NONE,
		FUNCTION
	}

	@Override
	public Void visitBlockStmt(Stmt.Block block) {
		beginScope();
		resolve(block.statements);
		endScope();
		return null;
	}

	@Override
	public Void visitVarStmt(Stmt.Var variable) {
		declare(variable.name);
		if(variable.initializer != null) {
			resolve(variable.initializer);
		}
		define(variable.name);
		return null;
	}

	@Override
	public Void visitVariableExpr(Expr.Variable variable) {
		if(!this.scopes.isEmpty() && this.scopes.peek().get(variable.name.lexeme) == Boolean.FALSE) {
			Main.error(variable.name, "Can't read local variable in its own initializer.");
		}

		resolveLocal(variable, variable.name);
		return null;
	}

	@Override
	public Void visitAssignExpr(Expr.Assign assignment) {
		resolve(assignment.value);
		resolveLocal(assignment, assignment.name);
		return null;
	}

	@Override
	public Void visitFunctionStmt(Stmt.Function function) {
		declare(function.name);
		define(function.name);
		resolveFunction(function, FunctionType.FUNCTION);
		return null;
	}

	@Override
	public Void visitExpressionStmt(Stmt.Expression statement) {
		resolve(statement.expression);
		return null;
	}

	@Override
	public Void visitIfStmt(Stmt.If statement) {
		resolve(statement.condition);
		resolve(statement.thenBranch);
		if(statement.elseBranch != null) resolve(statement.elseBranch);
		return null;
	}

	@Override
	public Void visitPrintStmt(Stmt.Print statement) {
		resolve(statement.expression);
		return null;
	}

	@Override
	public Void visitReturnStmt(Stmt.Return statement) {
		if(this.currentFunction == FunctionType.NONE) {
			Main.error(statement.keyword, "Can't return from top-level code.");
		}

		if(statement.value != null) {
			resolve(statement.value);
		}
		return null;
	}

	@Override
	public Void visitWhileStmt(Stmt.While statement) {
		resolve(statement.condition);
		resolve(statement.body);
		return null;
	}

	@Override
	public Void visitBinaryExpr(Expr.Binary binary) {
		resolve(binary.left);
		resolve(binary.right);
		return null;
	}

	@Override
	public Void visitCallExpr(Expr.Call call) {
		resolve(call.callee);
		for(Expr argument : call.arguments) {
			resolve(argument);
		}
		return null;
	}

	@Override
	public Void visitGroupingExpr(Expr.Grouping group) {
		resolve(group.expression);
		return null;
	}

	@Override
	public Void visitLiteralExpr(Expr.Literal literal) {
		return null;
	}

	@Override
	public Void visitLogicalExpr(Expr.Logical logic) {
		resolve(logic.left);
		resolve(logic.right);
		return null;
	}

	@Override
	public Void visitUnaryExpr(Expr.Unary unary) {
		resolve(unary.right);
		return null;
	}

	private void resolveLocal(Expr expression, Token name) {
		for(int i = this.scopes.size() - 1; i >= 0; i--) {
			if(this.scopes.get(i).containsKey(name.lexeme)) {
				this.interpreter.resolve(expression, this.scopes.size()-1-i);
				return;
			}
		}
	}

	private void resolveFunction(Stmt.Function function, FunctionType type) {
		FunctionType enclosingFunction = this.currentFunction;
		currentFunction = type;
		
		beginScope();
		for(Token param : function.params) {
			declare(param);
			define(param);
		}
		resolve(function.body);
		endScope();

		this.currentFunction = enclosingFunction;
	}

	private void resolve(Stmt statement) {
		statement.accept(this);
	}

	void resolve(List<Stmt> statements) {
		for(Stmt statement : statements) {
			resolve(statement);
		}
	}

	private void resolve(Expr expression) {
		expression.accept(this);
	}

	private void declare(Token name) {
		if(this.scopes.isEmpty()) return;
		Map<String, Boolean> scope = this.scopes.peek();
		if(scope.containsKey(name.lexeme)) {
			Main.error(name, "Already a variable with this name in this scope.");
		}
		scope.put(name.lexeme, false);
	}

	private void define(Token name) {
		if(this.scopes.isEmpty()) return;
		this.scopes.peek().put(name.lexeme, true);
	}

	private void beginScope() {
		this.scopes.push(new HashMap<String, Boolean>());
	}

	private void endScope() {
		this.scopes.pop();
	}
}