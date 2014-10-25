package org.pac4j.play;

public class HTTPActionRequiredException extends Exception {

	private static final long serialVersionUID = 1L;

	private int code;
	private String content;

	public HTTPActionRequiredException(int code, String content) {
		this.code = code;
		this.content = content;
	}

	public HTTPActionRequiredException(int code, String content, String message) {
		super(message);
		this.code = code;
		this.content = content;
	}

	public HTTPActionRequiredException(int code, String content, Throwable cause) {
		super(cause);
		this.code = code;
		this.content = content;
	}

	public HTTPActionRequiredException(int code, String content, String message, Throwable cause) {
		super(message, cause);
		this.code = code;
		this.content = content;
	}

	public HTTPActionRequiredException(int code, String content, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		this.code = code;
		this.content = content;
	}

	public int getCode() {
		return code;
	}

	public String getContent() {
		return content;
	}

}
