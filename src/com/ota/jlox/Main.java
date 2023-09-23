package com.ota.jlox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Main {
	private static final Interpreter interpreter = new Interpreter();

	static boolean hadError = false;
	static boolean hadRuntimeError = false;

	public static void main(String[] args) throws IOException {
		if(args.length > 1) {
			System.out.println("Usage: jlox [script]");
			System.exit(64);
		} else if(args.length == 0) {
			runPrompt();
		} else {
			runFile(args[0]);
		}
	}

	private static void runFile(String filepath) throws IOException {
		byte[] bytes = Files.readAllBytes(Paths.get(filepath));
		run(new String(bytes, Charset.defaultCharset()));

		if(hadError) System.exit(-1);
		if(hadRuntimeError) System.exit(-2);
	}

	private static void runPrompt() throws IOException {
		InputStreamReader input = new InputStreamReader(System.in);
		BufferedReader reader = new BufferedReader(input);

		while(true) {
			System.out.print("> ");
			String line = reader.readLine();
			if(line == null) break;
			run(line);
			hadError = false;
		}
	}

	private static void run(String source) {
		Lexer lexer = new Lexer(source);
		List<Token> tokens = lexer.scanTokens();

		Parser parser = new Parser(tokens);
		List<Stmt> statements = parser.parse();

		if(hadError) return;
		interpreter.interpret(statements);
		// System.out.println(new AstPrinter().print(expression));
	}

	static void error(int line, String message) {
		report(line, "", message);
	}

	private static void report(int line, String where, String message) {
		System.err.println("[line " + line + "] Error " + where + ": " + message);
		hadError = true;
	}

	static void error(Token token, String message) {
		String location = (token.type == TokenType.EOF) ?
			" at end" :
			" at '" + token.lexeme + "'";
		report(token.line, location, message);
	}

	static void runtimeError(RuntimeError error) {
		System.err.println(error.getMessage() + "\n[line " + error.token.line + "]");
		hadRuntimeError = true;
	}
}
