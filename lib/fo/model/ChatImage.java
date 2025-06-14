package org.mineacademy.fo.model;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.util.ChatPaginator;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.remain.CompChatColor;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

/**
 * Represents a way to show an image in chat
 *
 * @author bobacadodl and kangarko
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChatImage {

	/**
	 * Returns the default length, 8
	 */
	public final static int DEFAULT_HEIGHT = 8;

	/**
	 * Represents empty char
	 */
	private final static char TRANSPARENT_CHAR = ' ';

	/**
	 * Represents Minotar API endpoint from where we fetch the image
	 */
	@Getter
	@Setter
	public static String chatHeadEndpoint = "https://mc-heads.net/avatar/{PLAYER_NAME}/{HEIGHT}.png";

	/**
	 * The strategy to resize the image. By default, the TYPE_NEAREST_NEIGHBOR does not
	 * "smooth" edges, which is suitable for avatars and Minecraft assets. Change to
	 * {@link AffineTransformOp#TYPE_BILINEAR} to antialias the downsized image.
	 *
	 * @see AffineTransformOp
	 */
	@Getter
	@Setter
	private static int resizeMethod = AffineTransformOp.TYPE_NEAREST_NEIGHBOR;

	/**
	 * Edit the color used as background for PNG images, by default WHITE
	 */
	@Getter
	@Setter
	private static Color backgroundColor = Color.WHITE;

	/**
	 * Represents the currently loaded lines
	 */
	@Getter
	private String[] lines;

	/**
	 * Appends the given text next to the image
	 *
	 * @param text
	 * @return
	 */
	public ChatImage appendText(String... text) {
		for (int y = 0; y < this.lines.length; y++)
			if (text.length > y) {
				final String line = text[y];

				this.lines[y] += " " + line;
			}

		return this;
	}

	/**
	 * Appends the given text as centered, next to the image
	 * Beware that using formatting colors or unicode might break centering!
	 *
	 * @param text
	 * @return
	 */
	public ChatImage appendCenteredText(String... text) {
		for (int y = 0; y < this.lines.length; y++)
			if (text.length > y) {
				final int len = ChatPaginator.AVERAGE_CHAT_PAGE_WIDTH - this.lines[y].length();

				this.lines[y] = this.lines[y] + this.center(text[y], len);

			} else
				return this;

		return this;
	}

	/*
	 * Centers the given message according to the given length
	 */
	private String center(String message, int length) {
		if (message.length() > length)
			return message.substring(0, length);

		else if (message.length() == length)
			return message;

		else {
			final int leftPadding = (length - message.length()) / 2;
			final StringBuilder leftBuilder = new StringBuilder();

			for (int i = 0; i < leftPadding; i++)
				leftBuilder.append(" ");

			return leftBuilder.toString() + message;
		}
	}

	/**
	 * Sends this image to the given player
	 *
	 * @param sender
	 */
	public void send(CommandSender sender) {
		for (final String line : this.lines)
			sender.sendMessage(Variables.replace(line, sender));
	}

	/* ------------------------------------------------------------------------------- */
	/* Static */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Create a player head image from the player username. Uses DARK_SHADE font
	 * and {@link #DEFAULT_HEIGHT}. Invokes a blocking web request to
	 * {@link #chatHeadEndpoint} and throws an error on any failure.
	 *
	 * @param playerName
	 * @return
	 * @throws IOException
	 */
	public static ChatImage fromHead(String playerName) throws IOException {
		return fromHead(playerName, DEFAULT_HEIGHT);
	}

	/**
	 * Create a player head image from the player username. Uses DARK_SHADE font.
	 * Invokes a blocking web request to {@link #chatHeadEndpoint} and throws
	 * an error on any failure.
	 *
	 * @param playerName
	 * @param height
	 * @return
	 * @throws IOException
	 */
	public static ChatImage fromHead(String playerName, int height) throws IOException {
		return fromHead(playerName, height, ChatImage.Type.DARK_SHADE);
	}

	/**
	 * Create a player head image from the player username. Invokes a blocking web request
	 * to {@link #chatHeadEndpoint} and throws an error on any failure.
	 *
	 * @param playerName
	 * @param height
	 * @param characterType
	 * @return
	 * @throws IOException
	 */
	public static ChatImage fromHead(String playerName, int height, Type characterType) throws IOException {
		return fromImage(chatHeadEndpoint.replace("{PLAYER_NAME}", playerName).replace("{HEIGHT}", String.valueOf(height)), height, characterType);
	}

	/**
	 * Create a chat image from the given remote URL, the {@link #DEFAULT_HEIGHT} and DARK_SHADE
	 * character type. Invokes a blocking web request and throws an error on any failure.
	 *
	 * @param webUrl
	 * @return
	 * @throws IOException
	 */
	public static ChatImage fromImage(String webUrl) throws IOException {
		return fromImage(webUrl, DEFAULT_HEIGHT);
	}

	/**
	 * Create a chat image from the given remote URL, the given line height and DARK_SHADE character type.
	 * Invokes a blocking web request and throws an error on any failure.
	 *
	 * @param webUrl
	 * @param height
	 * @return
	 * @throws IOException
	 */
	public static ChatImage fromImage(String webUrl, int height) throws IOException {
		return fromImage(webUrl, height, Type.DARK_SHADE);
	}

	/**
	 * Create a chat image from the given remote URL, the given line height and character type.
	 * Invokes a blocking web request and throws an error on any failure.
	 *
	 * @param webUrl
	 * @param height
	 * @param characterType
	 * @return
	 * @throws IOException
	 */
	public static ChatImage fromImage(@NonNull String webUrl, int height, Type characterType) throws IOException {
		final BufferedImage image = ImageIO.read(new URL(webUrl));

		if (image == null)
			throw new NullPointerException("Unable to load image from URL ");

		else
			return fromSource(image, height, characterType);
	}

	/**
	 * Create an image to show in a chat message from the given path
	 * in your plugin's JAR, with {@link #DEFAULT_HEIGHT} and DARK_SHADE character type.
	 *
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static ChatImage fromFile(@NonNull File file) throws IOException {
		return fromFile(file, DEFAULT_HEIGHT);
	}

	/**
	 * Create an image to show in a chat message from the given path
	 * in your plugin's JAR, with the given height and DARK_SHADE character type.
	 *
	 * @param file
	 * @param height
	 * @return
	 * @throws IOException
	 */
	public static ChatImage fromFile(@NonNull File file, int height) throws IOException {
		return fromFile(file, height, ChatImage.Type.DARK_SHADE);
	}

	/**
	 * Create an image to show in a chat message from the given path
	 * in your plugin's JAR, with the given height and the given character type.
	 *
	 * @param file
	 * @param height
	 * @param characterType
	 * @return
	 * @throws IOException
	 */
	public static ChatImage fromFile(@NonNull File file, int height, Type characterType) throws IOException {
		Valid.checkBoolean(file.exists(), "Cannot load image from non existing file " + file.toPath());

		final BufferedImage image = ImageIO.read(file);

		if (image == null)
			throw new NullPointerException("Unable to load image size " + file.length() + " bytes from " + file.toPath());

		else
			return fromSource(image, height, characterType);
	}

	/*
	 * Helper to load the image
	 */
	private static ChatImage fromSource(@NonNull BufferedImage image, int height, @NonNull Type characterType) {
		Valid.checkBoolean(height >= 2, "File image height must be equal or above 2");

		final BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
		newImage.createGraphics().drawImage(image, 0, 0, backgroundColor, null);

		final CompChatColor[][] chatColors = parseImage(newImage, height);
		final ChatImage chatImage = new ChatImage();

		chatImage.lines = parseColors(chatColors, characterType);

		return chatImage;
	}

	/**
	 * Return a chat image from finished lines that were already created from {@link #fromFile(File, int, Type)}
	 * Useful when loading from a disk file or a remote databsae
	 *
	 * @param lines
	 * @return
	 */
	public static ChatImage fromLines(String[] lines) {
		final ChatImage chatImage = new ChatImage();
		chatImage.lines = lines;

		return chatImage;
	}

	/*
	 * Parse the given image into chat colors
	 */
	private static CompChatColor[][] parseImage(BufferedImage newImage, int height) {
		final double ratio = (double) newImage.getHeight() / newImage.getWidth();
		int width = (int) (height / ratio);

		if (width > 10)
			width = 10;

		final BufferedImage resized = resizeImage(newImage, (int) (height / ratio), height);
		final CompChatColor[][] chatImg = new CompChatColor[resized.getWidth()][resized.getHeight()];

		for (int x = 0; x < resized.getWidth(); x++)
			for (int y = 0; y < resized.getHeight(); y++) {
				final int rgb = resized.getRGB(x, y);
				final CompChatColor closest = CompChatColor.getClosestLegacyColor(new Color(rgb, true));

				chatImg[x][y] = closest;
			}

		return chatImg;
	}

	/*
	 * Resize the given image
	 */
	private static BufferedImage resizeImage(BufferedImage originalImage, int width, int height) {
		final AffineTransform af = new AffineTransform();

		af.scale(
				width / (double) originalImage.getWidth(),
				height / (double) originalImage.getHeight());

		final AffineTransformOp operation = new AffineTransformOp(af, resizeMethod);

		return operation.filter(originalImage, null);
	}

	/*
	 * Parse the given 2D colors to fit lines
	 */
	private static String[] parseColors(CompChatColor[][] colors, Type imgchar) {
		final String[] lines = new String[colors[0].length];

		for (int y = 0; y < colors[0].length; y++) {
			String line = "";

			for (final CompChatColor[] color2 : colors) {
				final CompChatColor color = color2[y];

				line += color != null ? color2[y].toString() + imgchar : TRANSPARENT_CHAR;
			}

			lines[y] = line + ChatColor.RESET;
		}

		return lines;
	}

	/* ------------------------------------------------------------------------------- */
	/* Classes */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Represents common image characters
	 *
	 * @author bobacadodl
	 */
	public enum Type {

		BLOCK('\u2588'),
		DARK_SHADE('\u2593'),
		MEDIUM_SHADE('\u2592'),
		LIGHT_SHADE('\u2591');

		/**
		 * The character used to build the image
		 */
		@Getter
		private final char character;

		Type(char c) {
			this.character = c;
		}

		/**
		 * Return the character
		 *
		 * @return
		 */
		@Override
		public String toString() {
			return String.valueOf(this.character);
		}
	}
}