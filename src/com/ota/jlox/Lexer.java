package com.ota.jlox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Lexer {
	private final String source;
	private final List<Token> tokens = new ArrayList<>();
	private int start = 0;
	private int current = 0;
	private int line = 1;

	private static final Map<String, TokenType> keywords;

	static {
		keywords = new HashMap<>();
		keywords.put("and",    TokenType.AND);
		keywords.put("class",  TokenType.CLASS);
		keywords.put("else",   TokenType.ELSE);
		keywords.put("false",  TokenType.FALSE);
		keywords.put("for",    TokenType.FOR);
		keywords.put("fun",    TokenType.FUN);
		keywords.put("if",     TokenType.IF);
		keywords.put("nil",    TokenType.NIL);
		keywords.put("or",     TokenType.OR);
		keywords.put("print",  TokenType.PRINT);
		keywords.put("return", TokenType.RETURN);
		keywords.put("super",  TokenType.SUPER);
		keywords.put("this",   TokenType.THIS);
		keywords.put("true",   TokenType.TRUE);
		keywords.put("var",    TokenType.VAR);
		keywords.put("while",  TokenType.WHILE);
	}

	Lexer(String source) {
		this.source = source;
	}

	List<Token> scanTokens() {
		while(!isAtEnd()) {
			this.start = this.current;
			scanToken();
		}

		this.tokens.add(new Token(TokenType.EOF, "", null, line));
		return this.tokens;
	}

	private void scanToken() {
		char c = advance();
		switch(c) {
			case '(': addToken(TokenType.LEFT_PAREN); break;
			case ')': addToken(TokenType.RIGHT_PAREN); break;
			case '{': addToken(TokenType.LEFT_BRACE); break;
			case '}': addToken(TokenType.RIGHT_BRACE); break;
			case ',': addToken(TokenType.COMMA); break;
			case '.': addToken(TokenType.DOT); break;
			case '-': addToken(TokenType.MINUS); break;
			case '+': addToken(TokenType.PLUS); break;
			case ';': addToken(TokenType.SEMICOLON); break;
			case '*': addToken(TokenType.STAR); break;

			case '!':
				addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
				break;
			case '=':
				addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);
				break;
			case '<':
				addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
				break;
			case '>':
				addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
				break;

			case '/':
				if(match('/')) {
					while(peek() != '\n' && !isAtEnd()) advance();
				} else {
					addToken(TokenType.SLASH);
				}
				break;

			// ignore whitespaces
			case ' ':
			case '\r':
			case '\t': break;

			case '\n': this.line++; break;

			case '"': string(); break;

			case '0': case '1': case '2': case '3': case '4':
			case '5': case '6': case '7': case '8': case '9':
				number();
				break;

			default:
				if(isAlpha(c)) {
					identifier();
				} else {
					Main.error(line, "Unexpected character.");
				}
				break;
		}
	}

	private void string() {
		while(peek() != '"' && !isAtEnd()) {
			if(peek() == '\n') {
				this.line++;
			}
			advance();
		}

		if(isAtEnd()) {
			Main.error(line, "Unterminated string.");
			return;
		}

		// eat the closing ".
		advance();

		// value without surrounding quotes.
		String value = source.substring(this.start+1, this.current-1);
		addToken(TokenType.STRING, value);
	}

	private void number() {
		char c = peek();
		while(c >= '0' && c <= '9') { advance(); c = peek(); }

		if(peek() == '.' && peekNext() >= '0' && peekNext() <= '9') {
			// consume the .
			advance();
			c = peek();
			while(c >= '0' && c <= '9') { advance(); c = peek(); }
		}

		addToken(TokenType.NUMBER, Double.parseDouble(source.substring(this.start, this.current)));
	}

	private void identifier() {
		while(isAlphaNumeric(peek())) advance();

		String text = source.substring(this.start, this.current);
		TokenType type = Lexer.keywords.get(text);
		if(type == null) type = TokenType.IDENTIFIER;
		addToken(type);
	}

	private boolean isAtEnd() {
		return this.current >= this.source.length();
	}

	private char advance() {
		return this.source.charAt(this.current++);
	}

	private void addToken(TokenType type) {
		this.tokens.add(new Token(type, this.source.substring(this.start, this.current), null, this.line));
	}

	private void addToken(TokenType type, Object literal) {
		this.tokens.add(new Token(type, this.source.substring(this.start, this.current), literal, this.line));
	}

	private boolean match(char expected) {
		if(isAtEnd() || this.source.charAt(current) != expected) return false;
		this.current++;
		return true;
	}

	private boolean isAlpha(char c) {
		return (c >= 'a' && c <= 'z') ||
			   (c >= 'A' && c <= 'Z') ||
			   c == '_';
	}

	private boolean isAlphaNumeric(char c) {
		return isAlpha(c) || (c >= '0' && c <= '9');
	}

	private char peek() {
		if(isAtEnd()) return '\0';
		return this.source.charAt(this.current);
	}

	private char peekNext() {
		if(this.current + 1 >= source.length()) return '\0';
		return source.charAt(this.current+1);
	}

}
