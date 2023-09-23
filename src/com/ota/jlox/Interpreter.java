package com.ota.jlox;

import java.util.List;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

	private Environment environment = new Environment();

	public void interpret(List<Stmt> statements) {
		try {
			for(Stmt statement : statements) {
				execute(statement);
			}
		} catch(RuntimeError error) {
			Main.runtimeError(error);
		}
	}

	@Override
	public Void visitIfStmt(Stmt.If If) {
		if(isTruthy(evaluate(If.condition))) {
			execute(If.thenBranch);
		} else if(If.elseBranch != null) {
			execute(If.elseBranch);
		}
		return null;
	}

	@Override
	public Void visitWhileStmt(Stmt.While While) {
		while(isTruthy(evaluate(While.condition))) {
			execute(While.body);
		}
		return null;
	}

	@Override
	public Void visitBlockStmt(Stmt.Block block) {
		executeBlock(block.statements, new Environment(environment));
		return null;
	}

	@Override
	public Void visitVarStmt(Stmt.Var variable) {
		Object value = (variable.initializer == null) ? null : evaluate(variable.initializer);
		environment.define(variable.name.lexeme, value);
		return null;
	}

	@Override
	public Void visitExpressionStmt(Stmt.Expression expression) {
		evaluate(expression.expression);
		return null;
	}

	@Override
	public Void visitPrintStmt(Stmt.Print print) {
		Object value = evaluate(print.expression);
		System.out.println(stringify(value));
		return null;
	}

	@Override
	public Object visitVariableExpr(Expr.Variable variable) {
		return environment.get(variable.name);
	}

	@Override
	public Object visitAssignExpr(Expr.Assign assignment) {
		Object value = evaluate(assignment.value);
		environment.assign(assignment.name, value);
		return value;
	}

	@Override
	public Object visitLiteralExpr(Expr.Literal literal) {
		return literal.value;
	}

	@Override
	public Object visitGroupingExpr(Expr.Grouping group) {
		return evaluate(group.expression);
	}

	@Override
	public Object visitUnaryExpr(Expr.Unary unary) {
		Object value = evaluate(unary.right);

		switch(unary.operator.type) {
			case BANG:
				return !isTruthy(value);

			case MINUS:
				checkNumberOperand(unary.operator, value);
				return -(double)value;
			default:
				break;
		}

		return null;
	}

	@Override
	public Object visitBinaryExpr(Expr.Binary binary) {
		Object left = evaluate(binary.left);
		Object right = evaluate(binary.right);

		switch(binary.operator.type) {
			case MINUS:
				checkNumberOperands(binary.operator, left, right);
				return (double)left - (double)right;
			case SLASH:
				checkNumberOperands(binary.operator, left, right);
				return (double)left / (double)right;
			case STAR:
				checkNumberOperands(binary.operator, left, right);
				return (double)left * (double)right;
			case PLUS:
				if(left instanceof Double && right instanceof Double)
					return (double)left + (double)right;

				if(left instanceof String && right instanceof String)
					return (String)left + (String)right;

				throw new RuntimeError(binary.operator, "Operands must be two numbers or two strings.");

			case GREATER:
				checkNumberOperands(binary.operator, left, right);
				return (double)left > (double)right;
			case GREATER_EQUAL:
				checkNumberOperands(binary.operator, left, right);
				return (double)left >= (double)right;
			case LESS:
				checkNumberOperands(binary.operator, left, right);
				return (double)left < (double)right;
			case LESS_EQUAL:
				checkNumberOperands(binary.operator, left, right);
				return (double)left <= (double)right;

			case BANG_EQUAL:
				return !isEqual(left, right);
			case EQUAL_EQUAL:
				return isEqual(left, right);
			default:
				break;
		}

		return null;
	}

	@Override
	public Object visitLogicalExpr(Expr.Logical logical) {
		Object left = evaluate(logical.left);
		if(logical.operator.type == TokenType.OR) {
			if(isTruthy(left)) return left;
		} else {
			if(!isTruthy(left)) return left;
		}
		return evaluate(logical.right);
	}

	private void execute(Stmt statement) {
		statement.accept(this);
	}

	private void executeBlock(List<Stmt> statements, Environment env) {
		Environment previous = environment;
		try {
			environment = env;
			for(Stmt statement : statements) {
				execute(statement);
			}
		} finally {
			environment = previous;
		}
	}

	private Object evaluate(Expr expression) {
		return expression.accept(this);
	}

	private boolean isTruthy(Object value) {
		if(value == null) return false;
		if(value instanceof Boolean) return (boolean)value;
		return true;
	}

	private boolean isEqual(Object left, Object right) {
		if(left == null && right == null) return true;
		if(left == null) return false;

		return left.equals(right);
	}

	private void checkNumberOperand(Token operator, Object operand) {
		if(operand instanceof Double) return;
		throw new RuntimeError(operator, "Operand must be a number.");
	}

	private void checkNumberOperands(Token operator, Object left, Object right) {
		if(right instanceof Double && left instanceof Double) return;
		throw new RuntimeError(operator, "Operands must be number.");
	}

	private String stringify(Object obj) {
		if(obj == null) return "nil";

		if(obj instanceof Double) {
			String text = obj.toString();
			return (!text.endsWith(".0")) ? text : text.substring(0, text.length()-2);
		}

		return obj.toString();
	}

}
