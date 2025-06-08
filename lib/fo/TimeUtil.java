//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package org.mineacademy.fo;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Generated;
import lombok.NonNull;
import org.mineacademy.fo.model.Replacer;
import org.mineacademy.fo.settings.SimpleSettings;
import org.mineacademy.fo.settings.SimpleLocalization.Cases;

public final class TimeUtil {
	private static final Pattern TOKEN_PATTERN = Pattern.compile("(?:([0-9]+)\\s*y[a-z]*[,\\s]*)?(?:([0-9]+)\\s*mo[a-z]*[,\\s]*)?(?:([0-9]+)\\s*w[a-z]*[,\\s]*)?(?:([0-9]+)\\s*d[a-z]*[,\\s]*)?(?:([0-9]+)\\s*h[a-z]*[,\\s]*)?(?:([0-9]+)\\s*m[a-z]*[,\\s]*)?(?:([0-9]+)\\s*(?:s[a-z]*)?)?", 2);
	private static final String stringFormat = "dd/MM/yyyy HH:mm";
	private static final DateTimeFormatter formatoData = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

	public static long currentTimeSeconds() {
		return System.currentTimeMillis() / 1000L;
	}

	public static long currentTimeTicks() {
		return System.currentTimeMillis() / 50L;
	}

	public static String getFormattedDate() {
		return getFormattedDate(System.currentTimeMillis());
	}

	public static String getFormattedDate(long time) {
		return SimpleSettings.DATE_FORMAT.format(time);
	}

	public static String getFormattedDateShort() {
		return getFormattedDateShort(System.currentTimeMillis());
	}

	public static String getFormattedDateShort(long time) {
		return SimpleSettings.DATE_FORMAT_SHORT.format(time);
	}

	public static String getFormattedDateMonth() {
		return getFormattedDateMonth(System.currentTimeMillis());
	}

	public static String getFormattedDateMonth(long time) {
		return SimpleSettings.DATE_FORMAT_MONTH.format(time);
	}

	public static long toTicks(String humanReadableTime) {
		Valid.checkNotNull(humanReadableTime, "Time is null");
		long seconds = 0L;
		String[] split = humanReadableTime.split(" ");
		if (split.length < 2) {
			throw new IllegalArgumentException("Expected human readable time like '1 second', got '" + humanReadableTime + "' instead");
		} else {
			for(int i = 1; i < split.length; ++i) {
				String sub = split[i].toLowerCase();
				int multiplier = 0;
				long unit = 0L;
				boolean isTicks = false;

				try {
					multiplier = Integer.parseInt(split[i - 1]);
				} catch (NumberFormatException var11) {
					continue;
				}

				if (sub.startsWith("tick")) {
					isTicks = true;
				} else if (sub.startsWith("second")) {
					unit = 1L;
				} else if (sub.startsWith("minute")) {
					unit = 60L;
				} else if (sub.startsWith("hour")) {
					unit = 3600L;
				} else if (sub.startsWith("day")) {
					unit = 86400L;
				} else if (sub.startsWith("week")) {
					unit = 604800L;
				} else if (sub.startsWith("month")) {
					unit = 2629743L;
				} else if (sub.startsWith("year")) {
					unit = 31556926L;
				} else {
					if (!sub.startsWith("potato")) {
						throw new IllegalArgumentException("Must define date type! Example: '1 second' (Got '" + sub + "')");
					}

					unit = 1337L;
				}

				seconds += (long)multiplier * (isTicks ? 1L : unit * 20L);
			}

			return seconds;
		}
	}

	public static String formatTimeGeneric(long seconds) {
		long second = seconds % 60L;
		long minute = seconds / 60L;
		String hourMsg = "";
		if (minute >= 60L) {
			long hour = seconds / 60L / 60L;
			minute %= 60L;
			hourMsg = Cases.HOUR.formatWithCount(hour) + " ";
		}

		return hourMsg + (minute != 0L ? Cases.MINUTE.formatWithCount(minute) + " " : "") + Cases.SECOND.formatWithCount(second);
	}

	public static String formatTimeDays(long seconds) {
		long minutes = seconds / 60L;
		long hours = minutes / 60L;
		long days = hours / 24L;
		String var10000 = days != 0L ? Cases.DAY.formatWithCount(days) + " " : "";
		return var10000 + (hours % 24L != 0L ? Cases.HOUR.formatWithCount(hours % 24L) + " " : "") + (minutes % 60L != 0L ? Cases.MINUTE.formatWithCount(minutes % 60L) + " " : "") + Cases.SECOND.formatWithCount(seconds % 60L);
	}

	public static String formatTimeShort(long seconds) {
		long minutes = seconds / 60L;
		long hours = minutes / 60L;
		long days = hours / 24L;
		hours %= 24L;
		minutes %= 60L;
		seconds %= 60L;
		String var10000 = days > 0L ? days + "d " : "";
		return var10000 + (hours > 0L ? hours + "h " : "") + (minutes > 0L ? minutes + "m " : "") + seconds + "s";
	}

	public static String formatTimeColon(long seconds) {
		long minutes = seconds / 60L;
		long hours = minutes / 60L;
		long days = hours / 24L;
		hours %= 24L;
		minutes %= 60L;
		seconds %= 60L;
		String var10000 = days > 0L ? (days < 10L ? "0" : "") + days + ":" : "";
		return var10000 + (hours > 0L ? (hours < 10L ? "0" : "") + hours + ":" : "") + (minutes > 0L ? (minutes < 10L ? "0" : "") + minutes + ":" : "00:") + (seconds < 10L ? "0" : "") + seconds;
	}

	public static long toMilliseconds(String text) {
		Matcher matcher = TOKEN_PATTERN.matcher(text);
		long years = 0L;
		long months = 0L;
		long weeks = 0L;
		long days = 0L;
		long hours = 0L;
		long minutes = 0L;
		long seconds = 0L;
		boolean found = false;

		while(matcher.find()) {
			if (matcher.group() != null && !matcher.group().isEmpty()) {
				for(int i = 0; i < matcher.groupCount(); ++i) {
					if (matcher.group(i) != null && !matcher.group(i).isEmpty()) {
						found = true;
						break;
					}
				}

				if (found) {
					for(int i = 1; i < 8; ++i) {
						if (matcher.group(i) != null && !matcher.group(i).isEmpty()) {
							long output = Long.parseLong(matcher.group(i));
							if (i == 1) {
								checkLimit("years", output, 10);
								years = output;
							} else if (i == 2) {
								checkLimit("months", output, 1200);
								months = output;
							} else if (i == 3) {
								checkLimit("weeks", output, 400);
								weeks = output;
							} else if (i == 4) {
								checkLimit("days", output, 3100);
								days = output;
							} else if (i == 5) {
								checkLimit("hours", output, 2400);
								hours = output;
							} else if (i == 6) {
								checkLimit("minutes", output, 6000);
								minutes = output;
							} else if (i == 7) {
								checkLimit("seconds", output, 6000);
								seconds = output;
							}
						}
					}
					break;
				}
			}
		}

		if (!found) {
			throw new NumberFormatException("Date not found from: " + text);
		} else {
			return (seconds + minutes * 60L + hours * 3600L + days * 86400L + weeks * 7L * 86400L + months * 30L * 86400L + years * 365L * 86400L) * 1000L;
		}
	}

	private static void checkLimit(String type, long value, int maxLimit) {
		if (value > (long)maxLimit) {
			throw new IllegalArgumentException("Value type " + type + " is out of bounds! Max limit: " + maxLimit + ", given: " + value);
		}
	}

	public static String toSQLTimestamp() {
		return toSQLTimestamp(System.currentTimeMillis());
	}

	public static String toSQLTimestamp(long timestamp) {
		Date date = new Date(timestamp);
		return (new Timestamp(date.getTime())).toString();
	}

	public static long fromSQLTimestamp(String timestamp) {
		return Timestamp.valueOf(timestamp).getTime();
	}

	public static boolean isInTimeframe(@NonNull String time, boolean future) {
		if (time == null) {
			throw new NullPointerException("time is marked non-null but is null");
		} else {
			Calendar calendar = Calendar.getInstance();
			String[] months = new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
			String[] fullNameMonths = new String[]{"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};

			for(int i = 0; i < months.length; ++i) {
				time = time.replaceAll(months[i] + "\\b", fullNameMonths[i]);
			}

			time = Replacer.replaceArray(time, new Object[]{"year", calendar.get(1), "month", fullNameMonths[calendar.get(2)], "day", calendar.get(5), "hour", calendar.get(11), "minute", calendar.get(12), "second", calendar.get(13)});

			try {
				long timestamp = (new SimpleDateFormat("dd MMM yyyy, HH:mm")).parse(time).getTime();
				if (future) {
					if (System.currentTimeMillis() < timestamp) {
						return false;
					}
				} else if (System.currentTimeMillis() > timestamp) {
					return false;
				}
			} catch (ParseException ex) {
				Common.throwError(ex, new String[]{"Syntax error in time operator.", "Valid: 'dd MMM yyyy, HH:mm' with {year/month/day/hour/minute/second} variables.", "Got: " + time});
			}

			return true;
		}
	}

	public static String dateNow() {
		LocalDateTime dataCorrente = LocalDateTime.now();
		return formatoData.format(dataCorrente);
	}

	public static boolean isExpired(String dataIngresso, int EXPIRING_DAYS) {
		if (dataIngresso == null) {
			return false;
		} else {
			try {
				LocalDate dataCorrente = LocalDate.now();
				LocalDate dataInput = LocalDate.parse(dataIngresso, formatoData);
				long differenzaInGiorni = ChronoUnit.DAYS.between(dataInput, dataCorrente);
				return differenzaInGiorni >= (long)EXPIRING_DAYS;
			} catch (DateTimeParseException e) {
				e.printStackTrace();
				return false;
			}
		}
	}

	public static String getExpiringTime(String dataScadenza) {
		if (dataScadenza == null) {
			return null;
		} else {
			try {
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Europe/Rome"));
				LocalDateTime dataScadenzaDateTime = LocalDateTime.parse(dataScadenza, formatter);
				LocalDateTime dataCorrenteDateTime = LocalDateTime.now(ZoneId.of("Europe/Rome"));
				Duration duration = Duration.between(dataCorrenteDateTime, dataScadenzaDateTime);
				if (duration.isNegative()) {
					return null;
				} else {
					long days = duration.toDays();
					long hours = (long)duration.toHoursPart();
					long minutes = (long)duration.toMinutesPart();
					return days + " giorni " + hours + " ore " + minutes + " minuti";
				}
			} catch (DateTimeParseException e) {
				e.printStackTrace();
				return null;
			}
		}
	}

	public static String getExpirationDay(String dataIngresso, int EXPIRING_DAYS) {
		if (dataIngresso == null) {
			return "";
		} else {
			try {
				LocalDateTime dataInput = LocalDateTime.parse(dataIngresso, DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
				LocalDateTime dataScadenza = dataInput.plusDays(EXPIRING_DAYS);
				return dataScadenza.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
			} catch (DateTimeParseException e) {
				e.printStackTrace();
				return null;
			}
		}
	}

	@Generated
	private TimeUtil() {
	}
}
