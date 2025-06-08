package org.mineacademy.fo.remain.nbt;

import java.lang.reflect.Constructor;

import org.mineacademy.fo.Common;

/**
 * This Enum wraps Constructors for NMS classes
 *
 * @author tr7zw
 *
 */

public enum ObjectCreator {
	NMS_NBTTAGCOMPOUND(null, null, ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz()),
	NMS_CUSTOMDATA(MinecraftVersion.MC1_20_R4, null, ClassWrapper.NMS_CUSTOMDATA.getClazz(), ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz()),
	NMS_BLOCKPOSITION(null, null, ClassWrapper.NMS_BLOCKPOSITION.getClazz(), int.class, int.class, int.class),
	NMS_COMPOUNDFROMITEM(MinecraftVersion.MC1_11_R1, MinecraftVersion.MC1_20_R3, ClassWrapper.NMS_ITEMSTACK.getClazz(),
			ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz()),;

	private Constructor<?> construct;
	private Class<?> targetClass;

	ObjectCreator(MinecraftVersion from, MinecraftVersion to, Class<?> clazz, Class<?>... args) {
		if ((clazz == null) || (from != null && MinecraftVersion.getVersion().getVersionId() < from.getVersionId()))
			return;
		if (to != null && MinecraftVersion.getVersion().getVersionId() > to.getVersionId())
			return;
		try {
			this.targetClass = clazz;
			this.construct = clazz.getDeclaredConstructor(args);
			this.construct.setAccessible(true);
		} catch (final Exception ex) {
			Common.error(ex, "Unable to find the constructor for the class '" + clazz.getName() + "'");
		}
	}

	/**
	 * Creates an Object instance with given args
	 *
	 * @param args
	 * @return Object created
	 */
	public Object getInstance(Object... args) {
		try {
			return this.construct.newInstance(args);
		} catch (final Exception ex) {
			throw new NbtApiException("Exception while creating a new instance of '" + this.targetClass + "'", ex);
		}
	}

}
