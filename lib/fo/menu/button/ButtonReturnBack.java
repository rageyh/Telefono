package org.mineacademy.fo.menu.button;

import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.SimpleLocalization;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Represents a standardized button that will return back to the parent menu
 */
@RequiredArgsConstructor
@AllArgsConstructor
public final class ButtonReturnBack extends Button {

	/**
	 * The material for this button, door by default
	 */
	@Getter
	@Setter
	private static CompMaterial material = CompMaterial.OAK_DOOR;

	/**
	 * The title of this button
	 */
	@Getter
	@Setter
	private static String title = SimpleLocalization.Menu.BUTTON_RETURN_TITLE;

	/**
	 * The lore of this button
	 */
	@Getter
	@Setter
	private static List<String> lore = Arrays.asList(SimpleLocalization.Menu.BUTTON_RETURN_LORE);

	/**
	 * You can override how this entire itemstack of the button will look like.
	 */
	@Getter
	@Setter
	private static ItemStack itemStack = null;

	/**
	 * The parent menu
	 */
	@NonNull
	private final Menu parentMenu;

	/**
	 * Should we make a new instance of the parent menu?
	 * <p>
	 * False by default.
	 */
	private boolean makeNewInstance = false;

	/**
	 * The icon for this button
	 */
	@Override
	public ItemStack getItem() {

		if (itemStack == null)
			return ItemCreator.of(material).name(title).lore(lore).make();

		return itemStack;
	}

	/**
	 * Open the parent menu when clicked
	 */
	@Override
	public void onClickedInMenu(Player player, Menu menu, ClickType click) {

		if (this.makeNewInstance) {

			// Flush data so that the parent menu can call the saved data in the current menu
			//
			// Example: In the Boss plugin, players can create new submenus and returning back to the main
			// menu they were not able to see the new submenus in the list before this change.
			final Inventory currentChestInventory = Remain.getTopInventoryFromOpenInventory(player);

			if (currentChestInventory != null)
				menu.handleClose(currentChestInventory);

			this.parentMenu.newInstance().displayTo(player);

		} else
			this.parentMenu.displayTo(player);
	}
}