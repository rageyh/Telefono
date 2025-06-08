package org.mineacademy.fo.menu;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.constants.FoConstants;
import org.mineacademy.fo.event.MenuCloseEvent;
import org.mineacademy.fo.event.MenuOpenEvent;
import org.mineacademy.fo.exception.EventHandledException;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.Button.DummyButton;
import org.mineacademy.fo.menu.button.ButtonReturnBack;
import org.mineacademy.fo.menu.button.StartPosition;
import org.mineacademy.fo.menu.button.annotation.Position;
import org.mineacademy.fo.menu.model.InventoryDrawer;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.model.MenuClickLocation;
import org.mineacademy.fo.model.SimpleRunnable;
import org.mineacademy.fo.model.SimpleSound;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompSound;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.SimpleLocalization;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * The core class of Menu. Represents a simple menu.
 *
 * <p>
 * This is the recommended menu class for all plugins having menus. It offers
 * having a parent menu, a return button and an info button explaining the
 * purpose of the menu to the user.
 *
 * <p>
 * HOW TO GET STARTED: Place final {@link Button} fields in your menu class and
 * make a instatiate when in constructor. Those will be registered as clickable
 * automatically. To render them, override {@link #getItemAt(int)} and make them
 * return at your desired positions.
 */
public abstract class Menu {

	// --------------------------------------------------------------------------------
	// Static
	// --------------------------------------------------------------------------------

	/**
	 * The default sound when switching between menus. Set to null to disable
	 */
	@Getter
	@Setter
	@Nullable
	private static SimpleSound sound = new SimpleSound(CompSound.BLOCK_NOTE_BLOCK_HAT.getSound(), .4F);

	/**
	 * Should we animate menu titles?
	 */
	@Getter
	@Setter
	private static boolean titleAnimationEnabled = true;

	/**
	 * The default duration of the new animated title before
	 * it is reverted back to the old one
	 * <p>
	 * Used in {@link #updateInventoryTitle(Menu, Player, String, String)}
	 */
	@Getter
	@Setter
	private static int titleAnimationDurationTicks = 20;

	/**
	 * A placeholder to represent that no item should be displayed/returned
	 */
	protected static final ItemStack NO_ITEM = null;

	// --------------------------------------------------------------------------------
	// Actual class
	// --------------------------------------------------------------------------------

	/**
	 * Automatically registered Buttons in this menu (using reflection)
	 */
	private final Map<Button, Position> registeredButtons = new HashMap<>();
	private final Map<Integer, Button> registeredButtonPositions = new HashMap<>();

	/**
	 * A list of manually registered buttons, in case you do not want to store them as fields.
	 */
	private final List<Button> buttons = new ArrayList<>();

	/**
	 * The registrator responsible for scanning the class and making buttons
	 * function
	 */
	private boolean buttonsRegistered = false;

	/**
	 * Parent menu
	 */
	private final Menu parent;

	/**
	 * The return button to the previous menu, null if none
	 */
	private final Button returnButton;

	// --------------------------------------------------------------------------------
	// Other constructors
	// --------------------------------------------------------------------------------

	/**
	 * The inventory title of the menu, colors & are supported
	 */
	private String title = "&0Menu";

	/**
	 * The size of the menu
	 */
	private Integer size = 9 * 3;

	/**
	 * The viewer of this menu, is null until {@link #displayTo(Player)} is called
	 */
	private Player viewer;

	/**
	 * Debug option to render empty spaces as glass panel having the slot id visible
	 */
	private boolean slotNumbersVisible;

	/**
	 * A one way boolean indicating this menu has been opened at least once
	 */
	private boolean opened = false;

	/**
	 * If you want to allow shift click on a menu item.
	 * Default will this return false.
	*/
	@Getter
	@Setter
	private boolean allowShift = false;
	/**
	 * Special case button only registered if this menu is {@link MenuQuantitable}
	 */
	@Nullable
	private final Button quantityButton;

	/**
	 * Create a new menu without parent menu with the size of 9*3
	 *
	 * <p>
	 * You are encouraged to change the size and title of this menu in your
	 * constructor by calling {@link #setTitle(String)} and
	 * {@link #setSize(Integer)}
	 *
	 * <p>
	 * NB: The {@link #getViewer()} of this menu is yet null!
	 */
	protected Menu() {
		this(null);
	}

	/**
	 * Create a new menu with parent menu with the size of 9*3
	 *
	 * <p>
	 * You are encouraged to change the size and title of this menu in your
	 * constructor by calling {@link #setTitle(String)} and
	 * {@link #setSize(Integer)}
	 *
	 * <p>
	 * NB: The {@link #getViewer()} of this menu is yet null!
	 *
	 * @param parent the parent menu
	 */
	protected Menu(final Menu parent) {
		this(parent, false);
	}

	/**
	 * Create a new menu with parent menu with the size of 9*3
	 *
	 * <p>
	 * You are encouraged to change the size and title of this menu in your
	 * constructor by calling {@link #setTitle(String)} and
	 * {@link #setSize(Integer)}
	 *
	 * <p>
	 * NB: The {@link #getViewer()} of this menu is yet null!
	 *
	 * @param parent                 the parent
	 * @param returnMakesNewInstance should we re-instatiate the parent menu when
	 *                               returning to it?
	 */
	protected Menu(final Menu parent, final boolean returnMakesNewInstance) {
		this.parent = parent;
		this.returnButton = parent != null ? new ButtonReturnBack(parent, returnMakesNewInstance) : Button.makeEmpty();
		this.quantityButton = this instanceof MenuQuantitable ? ((MenuQuantitable) this).getQuantityButton(this) : Button.makeEmpty();
	}

	/**
	 * Returns the current menu for player
	 *
	 * @param player the player
	 * @return the menu, or null if none
	 */
	public static final Menu getMenu(final Player player) {
		return getMenu0(player, FoConstants.NBT.TAG_MENU_CURRENT);
	}

	/**
	 * Returns the previous menu for player
	 *
	 * @param player the player
	 * @return the menu, or none
	 */
	public static final Menu getPreviousMenu(final Player player) {
		return getMenu0(player, FoConstants.NBT.TAG_MENU_PREVIOUS);
	}

	/**
	 * Returns the last closed menu, null if does not exist.
	 *
	 * @param player
	 * @return
	 */
	@Nullable
	public static final Menu getLastClosedMenu(final Player player) {
		if (player.hasMetadata(FoConstants.NBT.TAG_MENU_LAST_CLOSED)) {
			final Menu menu = (Menu) player.getMetadata(FoConstants.NBT.TAG_MENU_LAST_CLOSED).get(0).value();

			return menu;
		}

		return null;
	}

	// Returns the menu associated with the players metadata, or null
	private static Menu getMenu0(final Player player, final String tag) {
		if (player.hasMetadata(tag)) {
			final Menu menu = (Menu) player.getMetadata(tag).get(0).value();
			Valid.checkNotNull(menu, "Menu missing from " + player.getName() + "'s metadata '" + tag + "' tag!");

			return menu;
		}

		return null;
	}

	// --------------------------------------------------------------------------------
	// Reflection to make life easier
	// --------------------------------------------------------------------------------

	/**
	 * Registers a button to this menu manually without the button needing to be a field.
	 *
	 * DO NOT USE ON BUTTONS THAT ARE FIELDS, FIELD BUTTONS ARE AUTOMATICALLY REGISTERED
	 *
	 * @param button
	 */
	protected final void registerButton(final Button button) {
		Valid.checkBoolean(button.getSlot() != -1, "When calling registerButton, you must set the slot of the button either in the constructor or by overriding Button#getSlot()!");

		this.buttons.add(button);
	}

	/**
	 * Scans the menu class this menu extends and registers buttons
	 */
	final void registerButtons() {
		this.registeredButtons.clear();

		// Register buttons explicitly given
		{
			final List<Button> buttons = this.getButtonsToAutoRegister();

			if (buttons != null) {
				final Map<Button, Position> buttonsRemapped = new HashMap<>();

				for (final Button button : buttons)
					buttonsRemapped.put(button, null);

				this.registeredButtons.putAll(buttonsRemapped);
			}
		}

		// Register buttons from the list
		{
			for (final Button button : this.buttons)
				this.registeredButtons.put(button, null);
		}

		// Register buttons declared as fields
		{
			Class<?> lookup = this.getClass();

			do
				for (final Field f : lookup.getDeclaredFields())
					this.registerButton0(f);
			while (Menu.class.isAssignableFrom(lookup = lookup.getSuperclass()));
		}
	}

	// Scans the class and register fields that extend Button class
	private void registerButton0(final Field field) {
		field.setAccessible(true);

		final Class<?> type = field.getType();

		if (Button.class.isAssignableFrom(type)) {
			final Button button = (Button) ReflectionUtil.getFieldContent(field, this);

			Valid.checkNotNull(button, "Null button field named " + field.getName() + " in " + this);
			final Position position = field.getAnnotation(Position.class);

			this.registeredButtons.put(button, position);

		} else if (Button[].class.isAssignableFrom(type))
			throw new FoException("Button[] is no longer supported in menu for " + this.getClass());
	}

	/*
	 * Utility method to register buttons if they yet have not been registered
	 *
	 * This method will only register them once until the server is reset
	 */
	private final void registerButtonsIfHasnt() {
		if (!this.buttonsRegistered) {
			this.registerButtons();

			this.buttonsRegistered = true;
		}
	}

	/**
	 * Returns a list of buttons that should be registered manually.
	 *
	 * NOTICE: Button fields in your class are registered automatically, do not add
	 * them here
	 *
	 * @return button list, null by default
	 */
	protected List<Button> getButtonsToAutoRegister() {
		return null;
	}

	/**
	 * Attempts to find a button having the same icon as the given item stack.
	 *
	 * @param fromItem the itemstack to compare to
	 * @return the buttor or null if not found
	 *
	 * @deprecated use Position annotation or Button#getSlot instead because comparing by items can return a survival
	 *             item as button when the button is the same item with the same meta
	 */
	@Deprecated
	@Nullable
	protected final Button getButton(final ItemStack fromItem) {
		this.registerButtonsIfHasnt();

		for (final Map.Entry<Button, Position> entry : this.registeredButtons.entrySet()) {
			final Button button = entry.getKey();
			final Position position = entry.getValue();

			Valid.checkNotNull(button, "Menu button is null at " + this.getClass().getSimpleName());

			if (position == null && button.getSlot() == -1 && ItemUtil.isSimilar(fromItem, button.getItem()))
				return button;
		}

		return null;
	}

	/**
	 * Return a button at a certain slot from its {@link Position} annotation or {@link Button#getSlot()}
	 *
	 * @param slot
	 * @return
	 */
	@Nullable
	protected final Button getButton(final int slot) {
		this.registerButtonsIfHasnt();

		// Cannot put Button#getSlot into registeredButtonPositions because it can be dynamically set each time the menu is opened
		for (final Button button : this.registeredButtons.keySet()) {
			Valid.checkNotNull(button, "Menu button is null at " + this.getClass().getSimpleName());

			if (button.getSlot() != -1 && button.getSlot() == slot)
				return button;
		}

		return this.registeredButtonPositions.get(slot);
	}

	/**
	 * Return a new instance of this menu
	 *
	 * <p>
	 * You must override this in certain cases
	 *
	 * @return the new instance, of null
	 * @throws FoException if new instance could not be made, for example when the menu is
	 *            taking constructor params
	 */
	public Menu newInstance() {
		try {
			return ReflectionUtil.instantiate(this.getClass());
		} catch (final Throwable t) {
			try {
				final Object parent = this.getClass().getMethod("getParent").invoke(this.getClass());

				if (parent != null)
					return ReflectionUtil.instantiate(this.getClass(), parent);
			} catch (final Throwable tt) {
			}

			t.printStackTrace();
		}

		throw new FoException(this.getClass().getSimpleName() + " lacks newInstance() method! Store your constructor parameters as fields, "
				+ "override the method and return a new instance using fields as paramteres here. Example: https://i.imgur.com/5mqJ2nD.png");
	}

	// --------------------------------------------------------------------------------
	// Rendering the menu
	// --------------------------------------------------------------------------------

	/**
	 * Display this menu to the player
	 *
	 * @param player the player
	 */
	public final void displayTo(final Player player) {
		Valid.checkNotNull(this.size, "Size not set in " + this + " (call setSize in your constructor)");
		Valid.checkNotNull(this.title, "Title not set in " + this + " (call setTitle in your constructor)");

		if (MinecraftVersion.olderThan(V.v1_5)) {
			final String error = "Displaying menus require Minecraft 1.5.2 or greater.";

			if (Messenger.ENABLED)
				Messenger.error(player, error);
			else
				Common.tell(player, error);

			return;
		}

		this.viewer = player;
		this.registerButtonsIfHasnt();

		// Draw the menu
		final InventoryDrawer drawer = InventoryDrawer.of(this.size, this.title);

		// Allocate items
		this.compileItems().forEach((slot, item) -> drawer.setItem(slot, item));

		// Allow last minute modifications
		this.onPreDisplay(drawer);

		// Render empty slots as slot numbers if enabled
		this.debugSlotNumbers(drawer);

		// Call event after items have been set to allow to get them
		if (!Common.callEvent(new MenuOpenEvent(this, drawer, player)))
			return;

		// Prevent menu in conversation
		if (player.isConversing()) {
			player.sendRawMessage(Common.colorize(SimpleLocalization.Menu.CANNOT_OPEN_DURING_CONVERSATION));

			return;
		}

		// Play the pop sound
		if (sound != null)
			sound.play(player);

		// Register previous menu if exists
		{
			final Menu previous = getMenu(player);

			if (previous != null)
				player.setMetadata(FoConstants.NBT.TAG_MENU_PREVIOUS, new FixedMetadataValue(SimplePlugin.getInstance(), previous));
		}

		// Register current menu
		Common.runLater(1, () -> {
			try {
				this.onDisplay(drawer, player);

			} catch (final Throwable t) {
				Common.error(t, "Error opening menu " + Menu.this);

				return;
			}

			player.setMetadata(FoConstants.NBT.TAG_MENU_CURRENT, new FixedMetadataValue(SimplePlugin.getInstance(), Menu.this));

			this.opened = true;
			this.onPostDisplay(player);
		});
	}

	/**
	 * Sets all empty slots to light gray pane or adds a slot number to existing
	 * items lores if {@link #slotNumbersVisible} is true
	 *
	 * @param drawer
	 */
	private void debugSlotNumbers(final InventoryDrawer drawer) {
		if (this.slotNumbersVisible)
			for (int slot = 0; slot < drawer.getSize(); slot++) {
				final ItemStack item = drawer.getItem(slot);

				if (item == null)
					drawer.setItem(slot, ItemCreator.of(CompMaterial.LIGHT_GRAY_STAINED_GLASS_PANE, "Slot " + slot).make());
			}
	}

	/**
	 * Called automatically before the menu is displayed but after all items have
	 * been drawed
	 *
	 * <p>
	 * Override for custom last-minute modifications
	 *
	 * @param drawer the drawer
	 */
	protected void onPreDisplay(final InventoryDrawer drawer) {
	}

	/**
	 * Called when the menu is shown to the player, by default displays the menu
	 * from the inventory drawer
	 *
	 * @param drawer
	 * @param player
	 */
	protected void onDisplay(final InventoryDrawer drawer, final Player player) {
		drawer.display(player);
	}

	/**
	 * Called automatically after the menu is displayed to the viewer
	 *
	 * @param viewer
	 */
	protected void onPostDisplay(final Player viewer) {
	}

	/**
	 * Redraws and refreshes all buttons
	 */
	public final void restartMenu() {
		this.restartMenu(null);
	}

	/**
	 * Redraws and re-register all buttons while sending a title animation to the
	 * player
	 *
	 * @param animatedTitle the animated title
	 */
	public final void restartMenu(final String animatedTitle) {
		this.restartMenu(animatedTitle, true);
	}

	final void restartMenu(final String animatedTitle, final boolean callOnMenuClose) {

		final Player player = this.getViewer();
		Valid.checkNotNull(player, "Cannot restartMenu if it was not yet shown to a player! Menu: " + this);

		final Inventory inventory = Remain.getTopInventoryFromOpenInventory(player);
		Valid.checkBoolean(inventory.getType() == InventoryType.CHEST, player.getName() + "'s inventory closed in the meanwhile (now == " + inventory.getType() + ").");

		// Most plugins save items here
		if (callOnMenuClose)
			this.onMenuClose(player, inventory);

		this.registerButtons();

		// Call before calling getItemAt
		this.onRestartInternal();
		this.onRestart();

		final ItemStack[] content = inventory.getContents();
		final Map<Integer, ItemStack> newContent = this.compileItems();

		for (int i = 0; i < content.length; i++)
			content[i] = newContent.get(i);

		inventory.setContents(content);

		if (animatedTitle != null)
			this.animateTitle(animatedTitle);
	}

	/**
	 * Redraws buttons registered using {@link Position} annotation or having {@link Button#getSlot()} set
	 */
	public void redrawButtons() {

		// Redraw positions
		for (final Map.Entry<Integer, Button> entry : this.registeredButtonPositions.entrySet()) {
			final int slot = entry.getKey();
			final Button button = entry.getValue();

			this.setItem(slot, button.getItem());
		}

		// Redraw slots
		for (final Button button : this.registeredButtons.keySet())
			if (button.getSlot() != -1)
				this.setItem(button.getSlot(), button.getItem());
	}

	/*
	 * Internal hook before calling getItemAt
	 */
	void onRestartInternal() {
	}

	/**
	 * Called automatically when a menu is restarted. Called before getItemAt() and after registerButtons()
	 */
	public void onRestart() {
	}

	/**
	 * If you want to allow shift click on a menu item.
	 * Default will this return false. Compare to the
	 * {@link #isAllowShift()} method you can with this
	 * method only allow the click in specific slots.
	 *
	 * @param slot the slot player clicking on.
	 * @return true if allow the click.
	 */
	public boolean isAllowShift(int slot) {
		return false;
	}

	/**
	 * Draws the bottom bar for the player inventory
	 *
	 * @return
	 */
	private Map<Integer, ItemStack> compileItems() {
		this.registeredButtonPositions.clear();
		final Map<Integer, ItemStack> items = new HashMap<>();

		final boolean hasReturnButton = this.addReturnButton() && !(this.returnButton instanceof DummyButton);

		// Begin with basic items
		for (int slot = 0; slot < this.size; slot++) {
			ItemStack item = this.getItemAt(slot);

			if (item != null && CompMaterial.isAir(item))
				item = null;

			items.put(slot, item);
		}

		// Override by buttons
		for (final Map.Entry<Button, Position> entry : this.registeredButtons.entrySet()) {
			final Button button = entry.getKey();
			final Position position = entry.getValue();

			if (button.getSlot() != -1) {
				items.put(button.getSlot(), button.getItem());

			} else if (position != null) {
				int slot = position.value();
				final StartPosition startPosition = position.start();

				if (startPosition == StartPosition.CENTER)
					slot += this.getCenterSlot();

				else if (startPosition == StartPosition.BOTTOM_CENTER)
					slot += this.getSize() - 5;

				else if (startPosition == StartPosition.BOTTOM_LEFT)
					slot += this.getSize() - (hasReturnButton ? 2 : 1);

				else if (startPosition == StartPosition.TOP_LEFT)
					slot += 0;
				else
					throw new FoException("Does not know how to implement button position's Slot." + startPosition);

				this.registeredButtonPositions.put(slot, button);
				items.put(slot, button.getItem());
			}
		}

		// Add quantity edit button
		if (this instanceof MenuQuantitable) {
			final int slot = ((MenuQuantitable) this).getQuantityButtonPosition();

			if (slot != -1)
				items.put(slot, this.quantityButton.getItem());
		}

		// Override by hotbar
		{
			if (this.addInfoButton() && this.getInfo() != null)
				items.put(this.getInfoButtonPosition(), Button.makeInfo(this.getInfo()).getItem());

			if (hasReturnButton)
				items.put(this.getReturnButtonPosition(), this.returnButton.getItem());
		}

		return items;

	}

	// --------------------------------------------------------------------------------
	// Convenience messenger functions
	// --------------------------------------------------------------------------------

	/**
	 * Send a message to the viewer
	 *
	 * @param messages
	 */
	public final void tell(final String... messages) {
		Common.tell(this.viewer, messages);
	}

	/**
	 * Send a message to the viewer
	 *
	 * @param message
	 */
	public final void tellInfo(final String message) {
		Messenger.info(this.viewer, message);
	}

	/**
	 * Send a message to the viewer
	 *
	 * @param message
	 */
	public final void tellSuccess(final String message) {
		Messenger.success(this.viewer, message);
	}

	/**
	 * Send a message to the viewer
	 *
	 * @param message
	 */
	public final void tellWarn(final String message) {
		Messenger.warn(this.viewer, message);
	}

	/**
	 * Send a message to the viewer
	 *
	 * @param message
	 */
	public final void tellError(final String message) {
		Messenger.error(this.viewer, message);
	}

	/**
	 * Send a message to the viewer
	 *
	 * @param message
	 */
	public final void tellQuestion(final String message) {
		Messenger.question(this.viewer, message);
	}

	/**
	 * Send a message to the viewer
	 *
	 * @param message
	 */
	public final void tellAnnounce(final String message) {
		Messenger.announce(this.viewer, message);
	}

	// --------------------------------------------------------------------------------
	// Animations
	// --------------------------------------------------------------------------------

	/**
	 * Animate the title of this menu
	 *
	 * <p>
	 * Automatically reverts back to the old title after 1 second
	 *
	 * @param title the title to animate
	 */
	public void animateTitle(final String title) {
		if (titleAnimationEnabled)
			PlayerUtil.updateInventoryTitle(this, this.getViewer(), title, this.getTitle(), titleAnimationDurationTicks);
	}

	/**
	 * Start a repetitive task with the given period in ticks on the main thread,
	 * that is automatically stopped if the viewer no longer sees this menu.
	 *
	 * Can impose a performance penalty. Use cancel() to cancel.
	 *
	 * IMPORTANT TIPS:
	 *
	 * 1. To update buttons, set their slots via {@link Position} or {@link Button#getSlot()} and then call {@link #redrawButtons()}.
	 * 2. To animate items more effectivelly, create a new class in your plugin implementing Runnable, iterate for all
	 *    players and call {@link Menu#getMenu(Player)} for each. Then check if the menu is instance of your menu,
	 *    write onUpdate() method to that menu class and call it from your runnable instead.
	 *
	 * Example of a menu with animated button: https://i.imgur.com/z1VZDcw.png
	 *
	 * @param periodTicks
	 * @param task
	 */
	protected final void animate(final int periodTicks, final MenuRunnable task) {
		Valid.checkNotNull(this.viewer, "Cannot call animate() before the menu is shown, call your method in onDisplay() method instead.");

		Common.runTimer(2, periodTicks, this.wrapAnimation(task));
	}

	/**
	 * Start a repetitive task with the given period in ticks ASYNC,
	 * that is automatically stopped if the viewer no longer sees this menu.
	 *
	 * Use cancel() to cancel.
	 *
	 * IMPORTANT TIPS:
	 *
	 * 1. To update buttons, set their slots via {@link Position} or {@link Button#getSlot()} and then call {@link #redrawButtons()}.
	 * 2. To animate items more effectivelly, create a new class in your plugin implementing Runnable, iterate for all
	 *    players and call {@link Menu#getMenu(Player)} for each. Then check if the menu is instance of your menu,
	 *    write onUpdate() method to that menu class and call it from your runnable instead.
	 *
	 * Example of a menu with animated button: https://i.imgur.com/z1VZDcw.png
	 *
	 * @param periodTicks
	 * @param task
	 */
	protected final void animateAsync(final int periodTicks, final MenuRunnable task) {
		Valid.checkNotNull(this.viewer, "Cannot call animate() before the menu is shown, call your method in onDisplay() method instead.");

		Common.runTimerAsync(2, periodTicks, this.wrapAnimation(task));
	}

	/*
	 * Helper method to create a bukkit runnable
	 */
	private SimpleRunnable wrapAnimation(final MenuRunnable task) {
		return new SimpleRunnable() {
			boolean canceled = false;

			@Override
			public void run() {

				if (!Menu.this.opened) {
					if (!this.canceled)
						this.cancel();

					return;
				}

				try {
					task.run();

				} catch (final EventHandledException ex) {
					this.canceled = true;

					this.cancel();
				}
			}
		};
	}

	/**
	 * A special wrapper for animating menus
	 */
	@FunctionalInterface
	public interface MenuRunnable extends Runnable {

		/**
		 * Cancel the menu animation
		 */
		default void cancel() {
			throw new EventHandledException();
		}
	}

	// --------------------------------------------------------------------------------
	// Menu functions
	// --------------------------------------------------------------------------------

	/**
	 * Returns the item at a certain slot
	 *
	 * @param slot the slow
	 * @return the item, or null if no icon at the given slot (default)
	 */
	public ItemStack getItemAt(final int slot) {
		return NO_ITEM;
	}

	/**
	 * Get the info button position
	 *
	 * @return the slot which info buttons is located on
	 */
	protected int getInfoButtonPosition() {
		return this.size - 9;
	}

	/**
	 * Should we automatically add the return button to the bottom left corner?
	 *
	 * @return true if the return button should be added, true by default
	 */
	protected boolean addReturnButton() {
		return true;
	}

	/**
	 * Should we automatically add an info button {@link #getInfo()} at the
	 * {@link #getInfoButtonPosition()} ?
	 *
	 * @return
	 */
	protected boolean addInfoButton() {
		return true;
	}

	/**
	 * Get the return button position
	 *
	 * @return the slot which return buttons is located on
	 */
	protected int getReturnButtonPosition() {
		return this.size - 1;
	}

	/**
	 * Calculates the center slot of this menu
	 *
	 * <p>
	 * Credits to Gober at
	 * https://www.spigotmc.org/threads/get-the-center-slot-of-a-menu.379586/
	 *
	 * @return the estimated center slot
	 */
	protected final int getCenterSlot() {
		final int pos = this.size / 2;

		return this.size % 2 == 1 ? pos : pos - 5;
	}

	/**
	 * Return the middle slot in the last menu row (in the hotbar)
	 *
	 * @return
	 */
	protected final int getBottomCenterSlot() {
		return this.size - 5;
	}

	/**
	 * Should we prevent the click or drag?
	 *
	 * @param location the click location
	 * @param slot     the slot
	 * @param clicked  the clicked item
	 * @param cursor   the cursor
	 * @param action   the inventory action
	 *
	 * @return if the action is cancelled in the {@link InventoryClickEvent}, false
	 * by default
	 */
	protected boolean isActionAllowed(final MenuClickLocation location, final int slot, @Nullable final ItemStack clicked, @Nullable final ItemStack cursor, final InventoryAction action) {
		return this.isActionAllowed(location, slot, clicked, cursor);
	}

	/**
	 * Should we prevent the click or drag?
	 *
	 * @param location the click location
	 * @param slot     the slot
	 * @param clicked  the clicked item
	 * @param cursor   the cursor
	 * @param action   the inventory action
	 *
	 * @return if the action is cancelled in the {@link InventoryClickEvent}, false
	 * by default
	 */
	protected boolean isActionAllowed(final MenuClickLocation location, final int slot, @Nullable final ItemStack clicked, @Nullable final ItemStack cursor) {
		return false;
	}

	/**
	 * The title of this menu
	 *
	 * @return the menu title
	 */
	public final String getTitle() {
		return this.title;
	}

	/**
	 * Sets the title of this inventory, this change is reflected
	 * when this menu is already displayed to a given player.
	 *
	 * @param title the new title
	 */
	protected final void setTitle(final String title) {
		this.title = title;

		if (this.viewer != null && this.opened)
			PlayerUtil.updateInventoryTitle(this.viewer, title);
	}

	/**
	 * Return the parent menu or null
	 *
	 * @return
	 */
	public final Menu getParent() {
		return this.parent;
	}

	/**
	 * Get the size of this menu
	 *
	 * @return
	 */
	public final Integer getSize() {
		return this.size;
	}

	/**
	 * Sets the size of this menu (without updating the player container - if you
	 * want to update it call {@link #restartMenu()})
	 *
	 * @param size
	 */
	protected final void setSize(final Integer size) {
		this.size = size;
	}

	/**
	 * Set the menu's description
	 *
	 * <p>
	 * Used to create an info bottom in bottom left corner, see
	 * {@link Button#makeInfo(String...)}
	 *
	 * return info the info to set
	 */
	protected String[] getInfo() {
		return null;
	}

	/**
	 * Get the viewer that this instance of this menu is associated with
	 *
	 * @return the viewer of this instance, or null
	 */
	protected final Player getViewer() {
		return this.viewer;
	}

	/**
	 * Sets the viewer for this instance of this menu
	 *
	 * @param viewer
	 */
	protected final void setViewer(@NonNull final Player viewer) {
		this.viewer = viewer;
	}

	/**
	 * Return the top opened inventory if viewer exists
	 *
	 * @return
	 */
	protected final Inventory getInventory() {
		Valid.checkNotNull(this.viewer, "Cannot get inventory when there is no viewer!");

		final Inventory topInventory = Remain.getTopInventoryFromOpenInventory(this.viewer);
		Valid.checkNotNull(topInventory, "Top inventory is null!");

		return topInventory;
	}

	/**
	 * Get the open inventory content to match the array length, cloning items
	 * preventing ID mismatch in yaml files
	 *
	 * @param from
	 * @param to
	 * @return
	 */
	protected final ItemStack[] getContent(final int from, final int to) {
		final ItemStack[] content = this.getInventory().getContents();
		final ItemStack[] copy = new ItemStack[content.length];

		for (int i = from; i < copy.length; i++) {
			final ItemStack item = content[i];

			copy[i] = item != null ? item.clone() : null;
		}

		return Arrays.copyOfRange(copy, from, to);
	}

	/**
	 * Updates a slot in this menu. If your slot is a button and you want it to continue to function,
	 * use {@link Position} annotation or set the slot in the button itself.
	 *
	 * @param slot
	 * @param item
	 */
	protected final void setItem(final int slot, final ItemStack item) {
		final Inventory inventory = this.getInventory();

		inventory.setItem(slot, item);
	}

	/**
	 * If you wonder what slot numbers does each empty slot in your menu has then
	 * set this to true in your constructor
	 *
	 * <p>
	 * Only takes change when used in constructor or before calling
	 * {@link #displayTo(Player)} and cannot be updated in {@link #restartMenu()}
	 *
	 * @param visible
	 */
	protected final void setSlotNumbersVisible() {
		this.slotNumbersVisible = true;
	}

	/**
	 * Return if the given player is still viewing this menu, we compare
	 * the menu class of the menu the player is viewing and return true if both equal.
	 *
	 * @param player
	 * @return
	 */
	public final boolean isViewing(final Player player) {
		final Menu menu = Menu.getMenu(player);

		return menu != null && menu.getClass().getName().equals(this.getClass().getName());
	}

	// --------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------

	/**
	 * Called automatically when the menu is clicked.
	 *
	 * <p>
	 * By default we call the shorter {@link #onMenuClick(Player, int, ItemStack)}
	 * method.
	 *
	 * @param player    the player
	 * @param slot      the slot
	 * @param action    the action
	 * @param click     the click
	 * @param cursor    the cursor
	 * @param clicked   the item clicked
	 * @param cancelled is the event cancelled?
	 */
	protected void onMenuClick(final Player player, final int slot, final InventoryAction action, final ClickType click, final ItemStack cursor, final ItemStack clicked, final boolean cancelled) {
		this.onMenuClick(player, slot, clicked);
	}

	/**
	 * Called automatically when the menu is clicked
	 *
	 * @param player  the player
	 * @param slot    the slot
	 * @param clicked the item clicked
	 */
	protected void onMenuClick(final Player player, final int slot, final ItemStack clicked) {
	}

	/**
	 * Called automatically when a registered button is clicked
	 *
	 * <p>
	 * By default this method parses the click into
	 * {@link Button#onClickedInMenu(Player, Menu, ClickType)}
	 *
	 * @param player the player
	 * @param slot   the slot
	 * @param action the action
	 * @param click  the click
	 * @param button the button
	 */
	protected void onButtonClick(final Player player, final int slot, final InventoryAction action, final ClickType click, final Button button) {
		button.onClickedInMenu(player, this, click);
	}

	/**
	 * Handles the menu close, this does not close the inventory, only cleans up internally,
	 * do not use.
	 *
	 * @deprecated internal use only
	 * @param inventory
	 */
	@Deprecated
	public final void handleClose(final Inventory inventory) {
		this.viewer.removeMetadata(FoConstants.NBT.TAG_MENU_CURRENT, SimplePlugin.getInstance());
		this.viewer.setMetadata(FoConstants.NBT.TAG_MENU_LAST_CLOSED, new FixedMetadataValue(SimplePlugin.getInstance(), this));
		this.opened = false;

		this.onMenuClose(this.viewer, inventory);

		// End by calling API
		Common.callEvent(new MenuCloseEvent(this, inventory, this.viewer));
	}

	/**
	 * Called automatically when the menu is closed
	 *
	 * @param player    the player
	 * @param inventory the menu inventory that is being closed
	 */
	protected void onMenuClose(final Player player, final Inventory inventory) {
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "{}";
	}
}
