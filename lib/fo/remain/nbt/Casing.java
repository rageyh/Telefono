package org.mineacademy.fo.remain.nbt;

import java.util.function.UnaryOperator;

enum Casing {
	camelCase(s -> {
		if (s.length() < 2)
			return s.toLowerCase();
		return Character.toLowerCase(s.charAt(0)) + s.substring(1);
	}), snake_case(s -> {
		final StringBuilder result = new StringBuilder();
		// Convert the first letter to lowercase
		result.append(Character.toLowerCase(s.charAt(0)));
		// Iterate through the rest of the string
		for (int i = 1; i < s.length(); i++) {
			final char currentChar = s.charAt(i);
			// Convert uppercase letters to lowercase and add underscore
			if (Character.isUpperCase(currentChar))
				result.append('_').append(Character.toLowerCase(currentChar));
			else
				result.append(currentChar);
		}
		return result.toString();
	}), PascalCase(s -> {
		if (s.length() < 2)
			return s.toUpperCase();
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}), lowercase(String::toLowerCase), UPPERCASE(String::toUpperCase);

	private final UnaryOperator<String> convert;

	Casing(UnaryOperator<String> function) {
		this.convert = function;
	}

	public String convertString(String str) {
		return this.convert.apply(str);
	}
}