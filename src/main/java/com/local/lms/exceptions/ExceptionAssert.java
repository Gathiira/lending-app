package com.local.lms.exceptions;

public class ExceptionAssert {
	public static void throwException(String code, String message) {
		throw new BusinessException(code , message);
	}
	public static void throwException(String message) {
		throw new BusinessException(message);
	}
	public static void throwException(Boolean isTrue, String message) {
		if (Boolean.TRUE.equals(isTrue)) {
			throw new BusinessException(message);
		}
	}
}
