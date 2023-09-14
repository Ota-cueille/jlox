package com.ota.jlox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Main {
	static boolean hadError = false;

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

		for(Token token : tokens) {
			System.out.println(token);
		}
	}

	static void error(int line, String message) {
		report(line, "", message);
	}

	private static void report(int line, String where, String message) {
		System.err.println("[line " + line + "] Error " + where + ": " + message);
		hadError = true;
	}
}
