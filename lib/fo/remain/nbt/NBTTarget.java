package org.mineacademy.fo.remain.nbt;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(METHOD)
@interface NBTTarget {
	public String value();

	public Type type() default Type.AUTOMATIC;

	public enum Type {
		AUTOMATIC, GET, SET, HAS
	}
}
