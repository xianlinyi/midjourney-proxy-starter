package com.prechatting.exception;

public class ConnectionManuallyClosedException extends Exception {
	public ConnectionManuallyClosedException(String message) {
		super(message);
	}
}