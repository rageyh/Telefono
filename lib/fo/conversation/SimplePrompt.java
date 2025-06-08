package org.mineacademy.fo.conversation;

import org.bukkit.conversations.Conversable;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationPrefix;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.ValidatingPrompt;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.fo.settings.SimpleLocalization;

/**
 * Represents one question for the player during a server conversation
 */
public abstract class SimplePrompt extends ValidatingPrompt {

	/**
	 * Open the players menu back if any?
	 */
	private boolean openMenu = true;

	/**
	 * See {@link SimpleConversation#isModal()}
	 */

	/**
	 * The player who sees the input
	 */
	private Player player = null;

	protected SimplePrompt() {
	}

	/**
	 * Create a new prompt, show we open players menu back if he has any?
	 *
	 * @param openMenu
	 */
	protected SimplePrompt(final boolean openMenu) {
		this.openMenu = openMenu;
	}

	/**
	 * Return the prefix before tell messages
	 *
	 * @param ctx
	 * @return
	 */
	protected String getCustomPrefix() {
		return null;
	}

	/**
	 * @see {@link SimpleConversation#isModal()}
	 *
	 * @return
	 */
	protected boolean isModal() {
		return true;
	}

	/**
	 * @see SimpleConversation#setMenuAnimatedTitle(String)
	 *
	 * @return
	 */
	protected String getMenuAnimatedTitle() {
		return null;
	}

	/**
	 * Return the question, implemented in own way using colors
	 */
	@Override
	public final String getPromptText(final ConversationContext context) {
		String prompt = this.getPrompt(context);
		final String promptColorless = Common.stripColors(prompt);

		if (Messenger.ENABLED
				&& (this.getCustomPrefix() == null || !promptColorless.contains(Common.stripColors(this.getCustomPrefix())))
				&& !promptColorless.contains(Common.stripColors(Messenger.getSuccessPrefix())))
			prompt = Messenger.getQuestionPrefix() + prompt;

		return Variables.replace(prompt, this.getPlayer(context));
	}

	/**
	 * Return the question to the user in this prompt
	 *
	 * @param context
	 * @return
	 */
	protected abstract String getPrompt(ConversationContext context);

	/**
	 * Checks if the input from the user was valid, if it was, we can continue to the next prompt
	 *
	 * @param context
	 * @param input
	 * @return
	 */
	@Override
	protected boolean isInputValid(final ConversationContext context, final String input) {
		return true;
	}

	/**
	 * Return the failed error message when {@link #isInputValid(ConversationContext, String)} returns false
	 */
	@Override
	protected String getFailedValidationText(final ConversationContext context, final String invalidInput) {
		return null;
	}

	/**
	 * Converts the {@link ConversationContext} into a {@link Player}
	 * or throws an error if it is not a player
	 *
	 * @param ctx
	 * @return
	 */
	protected final Player getPlayer(final ConversationContext ctx) {
		Valid.checkBoolean(ctx.getForWhom() instanceof Player, "Conversable is not a player but: " + ctx.getForWhom());

		return (Player) ctx.getForWhom();
	}

	/**
	 * Send the player (in case any) the given message
	 *
	 * @param ctx
	 * @param message
	 */
	protected final void tell(final String message) {
		Valid.checkNotNull(this.player, "Cannot use tell() when player not yet set!");

		this.tell(this.player, message);
	}

	/**
	 * Send the player (in case any) the given message
	 *
	 * @param context
	 * @param message
	 */
	protected final void tell(final ConversationContext context, final String message) {
		this.tell(this.getPlayer(context), message);
	}

	/**
	 * Sends the message to the player
	 *
	 * @param conversable
	 * @param message
	 */
	protected final void tell(final Conversable conversable, final String message) {
		if (this.getCustomPrefix() != null)
			Common.tellConversingNoPrefix(conversable, this.getCustomPrefix() + message);
		else
			Common.tellConversing(conversable, message);
	}

	/**
	 * Sends the message to the player
	 *
	 * @param conversable
	 * @param message
	 */
	protected final void tellNoPrefix(final Conversable conversable, final String message) {
		Common.tellConversingNoPrefix(conversable, message);
	}

	/**
	 * Sends the message to the player later
	 *
	 * @param delayTicks
	 * @param conversable
	 * @param message
	 */
	protected final void tellLater(final int delayTicks, final Conversable conversable, final String message) {
		if (this.getCustomPrefix() != null)
			Common.tellLaterConversingNoPrefix(delayTicks, conversable, this.getCustomPrefix() + message);
		else
			Common.tellLaterConversing(delayTicks, conversable, message);
	}

	/**
	 * Sends the message to the player later
	 *
	 * @param delayTicks
	 * @param conversable
	 * @param message
	 */
	protected final void tellLaterNoPrefix(final int delayTicks, final Conversable conversable, final String message) {
		Common.tellLaterConversingNoPrefix(delayTicks, conversable, message);
	}

	/**
	 * Called when the whole conversation is over. This is called before onConversationEnd
	 *
	 * @param conversation
	 * @param event
	 */
	public void onConversationEnd(final SimpleConversation conversation, final ConversationAbandonedEvent event) {
	}

	// Do not allow superclasses to modify this since we have isInputValid here
	@Override
	public final Prompt acceptInput(final ConversationContext context, final String input) {
		try {
			// Since developers use try-catch blocks to validate input, do not save this as error
			FoException.setErrorSavedAutomatically(false);

			if (this.isInputValid(context, input))
				return this.acceptValidatedInput(context, input);

			else {
				final String failPrompt = this.getFailedValidationText(context, input);

				if (failPrompt != null) {
					final String failPromptColorless = Common.stripColors(failPrompt);
					final String prefixColorless = Common.stripColors(Messenger.getErrorPrefix());

					this.tellLaterNoPrefix(0, context.getForWhom(), Variables.replace((Messenger.ENABLED && !failPromptColorless.contains(prefixColorless) ? Messenger.getErrorPrefix() : "") + "&c" + failPrompt, this.getPlayer(context)));
				}

				// Redisplay this prompt to the user to re-collect input
				return this;
			}

		} finally {
			FoException.setErrorSavedAutomatically(true);
		}
	}

	/**
	 * Shows this prompt as a conversation to the player
	 * <p>
	 * NB: Do not call this as a means to showing this prompt DURING AN EXISTING
	 * conversation as it will fail! Use acceptValidatedInput instead
	 * to show the next prompt
	 *
	 * @param player
	 * @return
	 */
	public final Conversation show(final Player player) {
		Valid.checkBoolean(!player.isConversing(), "Player " + player.getName() + " is already conversing! Show them their next prompt in acceptValidatedInput() in " + this.getClass().getSimpleName() + " instead!");

		this.player = player;

		final SimpleConversation conversation = new SimpleConversation() {

			@Override
			protected Prompt getFirstPrompt() {
				return SimplePrompt.this;
			}

			@Override
			protected boolean isModal() {
				return SimplePrompt.this.isModal();
			}

			@Override
			protected ConversationPrefix getPrefix() {
				final String prefix = SimplePrompt.this.getCustomPrefix();

				return prefix != null ? new SimplePrefix(prefix) : super.getPrefix();
			}

			@Override
			public String getMenuAnimatedTitle() {
				return SimplePrompt.this.getMenuAnimatedTitle();
			}

			@Override
			protected void onConversationEnd(ConversationAbandonedEvent event, boolean canceledFromInactivity) {
				final String message = canceledFromInactivity ? SimpleLocalization.Conversation.CONVERSATION_CANCELLED_INACTIVE : SimpleLocalization.Conversation.CONVERSATION_CANCELLED;
				final Player player = SimplePrompt.this.getPlayer(event.getContext());

				if (!event.gracefulExit())
					if (Messenger.ENABLED)
						Messenger.warn(player, message);
					else
						Common.tell(player, message);
			}
		};

		if (this.openMenu) {
			final Menu menu = Menu.getMenu(player);

			if (menu != null)
				conversation.setMenuToReturnTo(menu);
		}

		return conversation.start(player);
	}

	/**
	 * Show the given prompt to the player
	 *
	 * @param player
	 * @param prompt
	 */
	public static final void show(final Player player, final SimplePrompt prompt) {
		prompt.show(player);
	}
}
