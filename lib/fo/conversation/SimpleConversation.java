package org.mineacademy.fo.conversation;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.bukkit.conversations.Conversable;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationAbandonedListener;
import org.bukkit.conversations.ConversationCanceller;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationPrefix;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.expiringmap.ExpiringMap;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.model.BoxedMessage;
import org.mineacademy.fo.model.SimpleTask;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompSound;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.SimpleLocalization;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * A simple way to communicate with the player
 * - their chat will be isolated and they chat messages processed and
 * the conversation input.
 */
public abstract class SimpleConversation implements ConversationAbandonedListener {

	/**
	 * The menu to return to, if any
	 */
	private Menu menuToReturnTo;

	/**
	 * Creates a simple conversation
	 */
	protected SimpleConversation() {
		this(null);
	}

	/**
	 * Creates a simple conversation that opens the
	 * menu when finished
	 *
	 * @param menuToReturnTo
	 */
	protected SimpleConversation(final Menu menuToReturnTo) {
		this.menuToReturnTo = menuToReturnTo;
	}

	/**
	 * Start a conversation with the player, throwing error if {@link Player#isConversing()}
	 *
	 * @param player
	 * @return
	 */
	public final CustomConversation start(final Player player) {
		Valid.checkBoolean(!player.isConversing(), "Player " + player.getName() + " is already conversing!");

		// Do not allow open inventory since they cannot type anyways
		player.closeInventory();

		// Setup
		final CustomConversation conversation = new CustomConversation(player);
		final CustomCanceller canceller = new CustomCanceller();

		canceller.setConversation(conversation);

		conversation.getCancellers().add(canceller);
		conversation.getCancellers().add(this.getCanceller());

		conversation.addConversationAbandonedListener(this);
		conversation.begin();

		return conversation;
	}

	/**
	 * Get the first prompt in this conversation for the player
	 *
	 * @return
	 */
	protected abstract Prompt getFirstPrompt();

	/**
	 * Listen for and handle existing the conversation
	 */
	@Override
	public final void conversationAbandoned(final ConversationAbandonedEvent event) {
		final ConversationContext context = event.getContext();
		final Conversable conversing = context.getForWhom();

		final Map<Object, Object> sessionData = Remain.getAllSessionData(context);

		final Object source = event.getSource();
		final boolean timeout = (boolean) sessionData.getOrDefault("FLP#TIMEOUT", false);

		// Remove the session data so that they are invisible to other plugnis
		sessionData.remove("FLP#TIMEOUT");

		if (source instanceof CustomConversation) {
			final SimplePrompt lastPrompt = ((CustomConversation) source).getLastSimplePrompt();

			if (lastPrompt != null)
				lastPrompt.onConversationEnd(this, event);
		}

		this.onConversationEnd(event, timeout);

		if (conversing instanceof Player) {
			final Player player = (Player) conversing;

			(event.gracefulExit() ? CompSound.ENTITY_ARROW_HIT_PLAYER : CompSound.BLOCK_NOTE_BLOCK_BASS).play(player, 1F, 1F);

			if (this.menuToReturnTo != null && this.reopenMenu()) {
				final Menu newMenu = this.menuToReturnTo.newInstance();

				newMenu.displayTo(player);

				final String title = this.getMenuAnimatedTitle();

				if (title != null)
					Common.runLater(2, () -> newMenu.animateTitle(title));
			}
		}
	}

	/**
	 * Fired when the user quits this conversation (see {@link #getCanceller()}, or
	 * simply quits the game)
	 *
	 *
	 * @param event
	 * @param canceledFromInactivity true if user failed to enter input in the period set in {@link #getTimeout()}
	 */
	protected void onConversationEnd(final ConversationAbandonedEvent event, boolean canceledFromInactivity) {
		this.onConversationEnd(event);
	}

	/**
	 * Fired when the user quits this conversation (see {@link #getCanceller()}, or
	 * simply quits the game)
	 *
	 * @param event
	 */
	protected void onConversationEnd(final ConversationAbandonedEvent event) {
	}

	/**
	 * Get conversation prefix before each message
	 * <p>
	 * By default we use the plugins tell prefix
	 * <p>
	 * TIP: You can use {@link SimplePrefix}
	 *
	 * @return
	 */
	protected ConversationPrefix getPrefix() {
		return new SimplePrefix(!Common.getTellPrefix().isEmpty() ? this.addLastSpace(Common.getTellPrefix()) : "");
	}

	/*
	 * Add a space to the prefix if it ends with one
	 */
	private final String addLastSpace(final String prefix) {
		return Common.stripColors(prefix).endsWith(" ") ? prefix : prefix + " ";
	}

	/**
	 * Return the canceller that listens for certain words to exit the convo,
	 * by default we use {@link SimpleCanceller} that listens to quit|cancel|exit
	 *
	 * @return
	 */
	protected ConversationCanceller getCanceller() {
		return new SimpleCanceller("quit", "cancel", "exit");
	}

	/**
	 * Return true if we should insert a prefix before each message, see {@link #getPrefix()}
	 *
	 * @return
	 */
	protected boolean insertPrefix() {
		return true;
	}

	/**
	 * If we detect the player has a menu opened should we reopen it?
	 *
	 * @return
	 */
	protected boolean reopenMenu() {
		return true;
	}

	/**
	 * Get the timeout in seconds before automatically exiting the convo
	 *
	 * @return
	 */
	protected int getTimeout() {
		return 60;
	}

	/**
	 * Modal true = Bukkit will hide all chat and plugin messages OTHER THAN
	 * conversational messages from your prompts
	 *
	 * Defaults to true
	 *
	 * @return
	 */
	protected boolean isModal() {
		return true;
	}

	/**
	 * Sets the menu to return to after the end of this conversation
	 *
	 * @param menu
	 */
	public final void setMenuToReturnTo(final Menu menu) {
		this.menuToReturnTo = menu;
	}

	/**
	 * The message that flashes in the menu title when opening it back to player
	 *
	 * @return the menuAnimatedTitle
	 */
	public String getMenuAnimatedTitle() {
		return null;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static access
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Sends a boxed message to the conversable player later
	 *
	 * @param delayTicks
	 * @param conversable
	 * @param messages
	 */
	protected static final void tellBoxed(final int delayTicks, final Conversable conversable, final String... messages) {
		Common.runLater(delayTicks, () -> tellBoxed(conversable, messages));
	}

	/**
	 * Sends a boxed message to the conversable player
	 *
	 * @param conversable
	 * @param messages
	 */
	protected static final void tellBoxed(final Conversable conversable, final String... messages) {
		BoxedMessage.tell((Player) conversable, messages);
	}

	/**
	 * Shortcut method for direct message send to the player
	 *
	 * @param conversable
	 * @param message
	 */
	protected static final void tell(final Conversable conversable, final String message) {
		Common.tellConversing(conversable, Variables.replace(message, (Player) conversable));
	}

	/**
	 * Send a message to the conversable player later
	 *
	 * @param delayTicks
	 * @param conversable
	 * @param message
	 */
	protected static final void tellLater(final int delayTicks, final Conversable conversable, final String message) {
		Common.tellLaterConversing(delayTicks, conversable, Variables.replace(message, (Player) conversable));
	}

	// ------------------------------------------------------------------------------------------------------------
	// Classes
	// ------------------------------------------------------------------------------------------------------------

	private final class CustomCanceller implements ConversationCanceller {

		protected Conversation conversation;

		private final int timeoutSeconds;
		private SimpleTask task = null;

		/**
		 */
		public CustomCanceller() {
			this.timeoutSeconds = SimpleConversation.this.getTimeout();
		}

		@Override
		public void setConversation(Conversation conversation) {
			this.conversation = conversation;

			this.startTimer();
		}

		@Override
		public boolean cancelBasedOnInput(ConversationContext context, String input) {
			this.stopTimer();
			this.startTimer();

			return false;
		}

		@Override
		public ConversationCanceller clone() {
			return new CustomCanceller();
		}

		/*
		 * Starts an inactivity timer.
		 */
		private void startTimer() {
			this.task = Common.runLater(this.timeoutSeconds * 20, (Runnable) () -> {
				if (this.conversation.getState() == Conversation.ConversationState.UNSTARTED)
					this.startTimer();

				else if (this.conversation.getState() == Conversation.ConversationState.STARTED) {
					this.conversation.getContext().setSessionData("FLP#TIMEOUT", true);

					this.conversation.abandon(new ConversationAbandonedEvent(this.conversation, this));
				}
			});
		}

		/*
		 * Stops the active inactivity timer.
		 */
		private void stopTimer() {
			if (this.task != null) {
				this.task.cancel();

				this.task = null;
			}
		}
	}

	/**
	 * Custom conversation class used for only showing the question once per 20 seconds interval
	 */
	private final class CustomConversation extends Conversation {

		/**
		 * Holds the information about the last prompt, used to invoke onConversationEnd
		 */
		@Getter(value = AccessLevel.PRIVATE)
		private SimplePrompt lastSimplePrompt;

		private CustomConversation(final Conversable forWhom) {
			super(SimplePlugin.getInstance(), forWhom, SimpleConversation.this.getFirstPrompt());

			this.localEchoEnabled = false;
			this.modal = SimpleConversation.this.isModal();

			if (SimpleConversation.this.insertPrefix() && SimpleConversation.this.getPrefix() != null)
				this.prefix = SimpleConversation.this.getPrefix();
		}

		@Override
		public void outputNextPrompt() {
			if (this.currentPrompt == null)
				try {
					this.abandon(new ConversationAbandonedEvent(this));

				} catch (final Throwable t) {
					tell(this.context.getForWhom(), (Messenger.ENABLED ? Messenger.getErrorPrefix() : "") + SimpleLocalization.Conversation.CONVERSATION_ERROR);

					t.printStackTrace();
				}

			else {
				// Save the time when we showed the question to the player
				// so that we only show it once per the given threshold
				final String promptClass = this.currentPrompt.getClass().getSimpleName();
				String question = this.currentPrompt.getPromptText(this.context);

				try {
					final ExpiringMap<String, Void /*dont have expiring set class*/> askedQuestions = (ExpiringMap<String, Void>) Remain.getAllSessionData(this.context).getOrDefault("Asked_" + promptClass, ExpiringMap.builder().expiration(SimpleConversation.this.getTimeout(), TimeUnit.SECONDS).build());

					if (!askedQuestions.containsKey(question)) {
						askedQuestions.put(question, null);

						if (!question.contains(Common.colorize(Messenger.getQuestionPrefix())))
							question = this.prefix.getPrefix(this.context) + question;

						this.context.setSessionData("Asked_" + promptClass, askedQuestions);
						this.context.getForWhom().sendRawMessage(question);
					}
				} catch (final NoSuchMethodError ex) {
					// Unfortunately, old MC version was detected
				}

				// Save last prompt if it is our class
				if (this.currentPrompt instanceof SimplePrompt)
					this.lastSimplePrompt = (SimplePrompt) this.currentPrompt;

				if (!this.currentPrompt.blocksForInput(this.context)) {
					this.currentPrompt = this.currentPrompt.acceptInput(this.context, null);
					this.outputNextPrompt();
				}
			}
		}
	}
}