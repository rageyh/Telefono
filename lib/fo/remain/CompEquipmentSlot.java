package org.mineacademy.fo.remain;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.menu.model.ItemCreator;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.citizensnpcs.api.trait.trait.Equipment.EquipmentSlot;

/**
 * Represents EquipmentSlot
 */
@RequiredArgsConstructor
public enum CompEquipmentSlot {

	HAND("HAND", "HAND"),
	/**
	 * Requires Minecraft 1.9+
	 */
	OFF_HAND("OFF_HAND", "OFF_HAND"),
	HEAD("HEAD", "HELMET"),
	CHEST("CHEST", "CHESTPLATE"),
	LEGS("LEGS", "LEGGINGS"),
	FEET("FEET", "BOOTS");

	/**
	 * Requires the entity to be a {@link Horse}
	 */
	//BODY("BODY", null);

	/**
	 * The localizable key
	 */
	@Getter
	private final String key;

	/**
	 * The alternative Bukkit name.
	 */
	private final String bukkitName;

	/**
	 * Applies this equipment slot to the given entity with the given item
	 *
	 * @param entity
	 * @param itemCreator
	 */
	public void applyTo(LivingEntity entity, ItemCreator itemCreator) {
		this.applyTo(entity, itemCreator.make(), null);
	}

	/**
	 * Applies this equipment slot to the given entity with the given item,
	 * and optional drop chance from 0 to 1.0
	 *
	 * @param entity
	 * @param itemCreator
	 * @param dropChance
	 */
	public void applyTo(LivingEntity entity, ItemCreator itemCreator, @Nullable Double dropChance) {
		this.applyTo(entity, itemCreator.make(), dropChance);
	}

	/**
	 * Applies this equipment slot to the given entity with the given item
	 *
	 * @param entity
	 * @param material
	 */
	public void applyTo(LivingEntity entity, CompMaterial material) {
		this.applyTo(entity, material.toItem(), null);
	}

	/**
	 * Applies this equipment slot to the given entity with the given item,
	 * and optional drop chance from 0 to 1.0
	 *
	 * @param entity
	 * @param material
	 * @param dropChance
	 */
	public void applyTo(LivingEntity entity, CompMaterial material, @Nullable Double dropChance) {
		this.applyTo(entity, material.toItem(), dropChance);
	}

	/**
	 * Applies this equipment slot to the given entity with the given item
	 *
	 * @param entity
	 * @param material
	 */
	public void applyTo(LivingEntity entity, Material material) {
		this.applyTo(entity, new ItemStack(material), null);
	}

	/**
	 * Applies this equipment slot to the given entity with the given item,
	 * and optional drop chance from 0 to 1.0
	 *
	 * @param entity
	 * @param material
	 * @param dropChance
	 */
	public void applyTo(LivingEntity entity, Material material, @Nullable Double dropChance) {
		this.applyTo(entity, new ItemStack(material), dropChance);
	}

	/**
	 * Applies this equipment slot to the given entity with the given item
	 *
	 * @param entity
	 * @param item
	 */
	public void applyTo(LivingEntity entity, ItemStack item) {
		this.applyTo(entity, item, null);
	}

	/**
	 * Clear this equipment slot.
	 *
	 * @param entity
	 */
	public void clear(LivingEntity entity) {
		this.applyTo(entity, (ItemStack) null, (Double) null);
	}

	/**
	 * Applies this equipment slot to the given entity with the given item,
	 * and optional drop chance from 0 to 1.0
	 *
	 * @param entity
	 * @param item
	 * @param dropChance
	 */
	public void applyTo(@NonNull LivingEntity entity, ItemStack item, @Nullable Double dropChance) {
		final EntityEquipment equipment = entity instanceof LivingEntity ? entity.getEquipment() : null;
		Valid.checkNotNull(equipment);

		final boolean lacksDropChance = entity instanceof HumanEntity || entity.getType().toString().equals("ARMOR_STAND");

		if (MinecraftVersion.olderThan(V.v1_9) && item == null)
			item = new ItemStack(Material.AIR);

		switch (this) {
			case HAND:
				if (entity instanceof Enderman) {
					final Enderman enderman = (Enderman) entity;

					if (item != null && item.getType().isBlock())
						try {
							enderman.setCarriedBlock(Bukkit.createBlockData(item.getType()));

						} catch (final Throwable t) {
							enderman.setCarriedMaterial(item.getData());
						}

				} else {
					equipment.setItemInHand(item);

					if (dropChance != null && !lacksDropChance)
						equipment.setItemInHandDropChance(dropChance.floatValue());
				}

				break;

			case OFF_HAND:
				Valid.checkBoolean(MinecraftVersion.atLeast(V.v1_9), "Setting off hand item requires Minecraft 1.9+");

				equipment.setItemInOffHand(item);

				if (dropChance != null && !lacksDropChance)
					equipment.setItemInOffHandDropChance(dropChance.floatValue());

				break;

			case HEAD:
				equipment.setHelmet(item);

				if (dropChance != null && !lacksDropChance)
					equipment.setHelmetDropChance(dropChance.floatValue());

				break;

			case CHEST:
				equipment.setChestplate(item);

				if (dropChance != null && !lacksDropChance)
					equipment.setChestplateDropChance(dropChance.floatValue());

				break;

			case LEGS:
				equipment.setLeggings(item);

				if (dropChance != null && !lacksDropChance)
					equipment.setLeggingsDropChance(dropChance.floatValue());

				break;

			case FEET:
				equipment.setBoots(item);

				if (dropChance != null && !lacksDropChance)
					equipment.setBootsDropChance(dropChance.floatValue());

				break;

			/*case BODY:
				Valid.checkBoolean(entity instanceof Horse, "Equipment slot BODY requires a Horse entity! Got " + entity.getType());

				((Horse) entity).getInventory().setArmor(item);
				break;*/
		}
	}

	/**
	 * Return the Bukkit name of this equipment
	 * or throw an error if not found
	 *
	 * @return
	 */
	public String getBukkitName() {
		Valid.checkNotNull(this.bukkitName, "CompEquipmentSlot." + name() + " does not have a Bukkit counterpart!");

		return this.bukkitName;
	}

	/**
	 * Return the Bukkit equipment slot of this equipment
	 * or throw an error if not found
	 *
	 * @return
	 */
	public EquipmentSlot toBukkit() {
		return ReflectionUtil.lookupEnum(EquipmentSlot.class, this.getBukkitName());
	}

	/**
	 * Attempts to parse equip. slot from the given key, or throwing
	 * an error if not found
	 *
	 * @param key
	 * @return
	 */
	public static CompEquipmentSlot fromKey(String key) {
		key = key.toUpperCase().replace(" ", "_");

		for (final CompEquipmentSlot slot : values())
			if (slot.key.equals(key) || slot.bukkitName.equals(key))
				return slot;

		throw new IllegalArgumentException("No such comp equipment slot: " + key + " Available: " + values());
	}

	/**
	 * A convenience shortcut to quickly give the entity a full leather armor in the given color
	 * that does not drop.
	 *
	 * @param entity
	 * @param color
	 */
	public static void applyArmor(LivingEntity entity, CompColor color) {
		applyArmor(entity, color, 0D, new HashSet<>());
	}

	/**
	 * A convenience shortcut to quickly give the entity a full leather armor in the given color
	 * that does not drop.
	 *
	 * @param entity
	 * @param color
	 * @param ignoredSlots
	 */
	public static void applyArmor(LivingEntity entity, CompColor color, Set<CompEquipmentSlot> ignoredSlots) {
		applyArmor(entity, color, 0D, ignoredSlots);
	}

	/**
	 * A convenience shortcut to quickly give the entity a full leather armor in the given color
	 *
	 * @param entity
	 * @param color
	 * @param dropChance
	 */
	public static void applyArmor(LivingEntity entity, CompColor color, double dropChance) {
		applyArmor(entity, color, dropChance, new HashSet<>());
	}

	/**
	 * A convenience shortcut to quickly give the entity a full leather armor in the given color
	 *
	 * @param entity
	 * @param color
	 * @param dropChance
	 * @param ignoredSlots
	 */
	public static void applyArmor(LivingEntity entity, CompColor color, Double dropChance, Set<CompEquipmentSlot> ignoredSlots) {
		if (!ignoredSlots.contains(HEAD))
			HEAD.applyTo(entity, ItemCreator.of(CompMaterial.LEATHER_HELMET).color(color).make(), dropChance);

		if (!ignoredSlots.contains(CHEST))
			CHEST.applyTo(entity, ItemCreator.of(CompMaterial.LEATHER_CHESTPLATE).color(color).make(), dropChance);

		if (!ignoredSlots.contains(LEGS))
			LEGS.applyTo(entity, ItemCreator.of(CompMaterial.LEATHER_LEGGINGS).color(color).make(), dropChance);

		if (!ignoredSlots.contains(FEET))
			FEET.applyTo(entity, ItemCreator.of(CompMaterial.LEATHER_BOOTS).color(color).make(), dropChance);
	}

	/**
	 * A convenience shortcut to quickly give the entity a full armor of the given type
	 * with 0 drop chance
	 *
	 * @param entity
	 * @param type
	 */
	public static void applyArmor(LivingEntity entity, Type type) {
		applyArmor(entity, type, 0d, new HashSet<>());
	}

	/**
	 * A convenience shortcut to quickly give the entity a full armor of the given type
	 * with 0 drop chance
	 *
	 * @param entity
	 * @param type
	 * @param ignoredSlots
	 */
	public static void applyArmor(LivingEntity entity, Type type, Set<CompEquipmentSlot> ignoredSlots) {
		applyArmor(entity, type, 0d, ignoredSlots);
	}

	/**
	 * A convenience shortcut to quickly give the entity a full armor of the given type
	 *
	 * @param entity
	 * @param type
	 * @param dropChance
	 */
	public static void applyArmor(LivingEntity entity, Type type, double dropChance) {
		applyArmor(entity, type, dropChance, new HashSet<>());
	}

	/**
	 * A convenience shortcut to quickly give the entity a full armor of the given type
	 *
	 * @param entity
	 * @param type
	 * @param dropChance
	 * @param ignoredSlots
	 */
	public static void applyArmor(LivingEntity entity, Type type, Double dropChance, Set<CompEquipmentSlot> ignoredSlots) {

		// Compatibility
		if (type == Type.NETHERITE && MinecraftVersion.olderThan(V.v1_16))
			type = Type.DIAMOND;

		final String name = type == Type.GOLD ? "GOLDEN" : type.toString();

		if (!ignoredSlots.contains(HEAD))
			HEAD.applyTo(entity, CompMaterial.valueOf(name + "_HELMET").toItem(), dropChance);

		if (!ignoredSlots.contains(CHEST))
			CHEST.applyTo(entity, CompMaterial.valueOf(name + "_CHESTPLATE").toItem(), dropChance);

		if (!ignoredSlots.contains(LEGS))
			LEGS.applyTo(entity, CompMaterial.valueOf(name + "_LEGGINGS").toItem(), dropChance);

		if (!ignoredSlots.contains(FEET))
			FEET.applyTo(entity, CompMaterial.valueOf(name + "_BOOTS").toItem(), dropChance);
	}

	@Override
	public String toString() {
		return this.key.toUpperCase();
	}

	/**
	 * Denotes the main armor material type such as Leather or Diamond
	 *
	 */
	public static enum Type {
		LEATHER,
		CHAINMAIL,
		IRON,
		GOLD,
		DIAMOND,
		NETHERITE;

		/**
		 * Attempts to parse armor material (any helmet, chestplate, leggings or boots)
		 * to a type based on its type (i.e. iron_helmet -> iron)
		 *
		 * @param armorMaterial
		 * @return
		 */
		public static Type fromArmor(CompMaterial armorMaterial) {
			final String n = armorMaterial.name();

			Valid.checkBoolean(n.contains("LEATHER") || n.contains("CHAINMAIL") || n.contains("IRON") || n.contains("GOLD") || n.contains("DIAMOND") || n.contains("NETHERITE"),
					"Only leather to netherite armors are supported, not: " + armorMaterial);

			return Type.valueOf(n.split("_")[0]);
		}
	}
}
