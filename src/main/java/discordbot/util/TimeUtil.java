package discordbot.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeUtil {
	private static final int SECOND_MILLIS = 1000;
	private static final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
	private static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
	private static final int DAY_MILLIS = 24 * HOUR_MILLIS;
	private static final SimpleDateFormat timestamp = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

	public static String getTimestampFormat(long time) {
		try {
			return timestamp.format(new Date(time));
		} catch (Exception e) {
			return "cant figure out (" + time + ")";
		}
	}

	/**
	 * @param time timestamp in seconds
	 * @return string of x min ago
	 */
	public static String getRelativeTime(long time) {
		return getRelativeTime(time, true);
	}

	/**
	 * @param time      timestamp in seconds
	 * @param shortText short or long text
	 * @return string of x min ago
	 */
	public static String getRelativeTime(long time, boolean shortText) {
		return getRelativeTime(time, shortText, true);
	}

	/**
	 * @param time           timestamp in seconds
	 * @param shortText      short or long texts
	 * @param showChronology show when it was (ago or from now)
	 * @return x m[inutes [from now]]
	 */
	public static String getRelativeTime(long time, boolean shortText, boolean showChronology) {
		long usedTime = time * 1000L;
		boolean future = false;
		String chronology = "ago";
		long now = System.currentTimeMillis();
		if (usedTime <= 0) {
			return "???";
		}
		long diff;
		if (usedTime > now) {
			diff = usedTime - now;
			chronology = "from now";
			future = true;
		} else {
			diff = now - usedTime;
		}
		if (!showChronology) {
			chronology = "";
		}
		if (diff < MINUTE_MILLIS) {
			return (diff / SECOND_MILLIS) + (shortText ? "s" : " seconds " + chronology);
		} else if (diff < 2 * MINUTE_MILLIS) {
			return shortText ? "~1m" : "about a minute " + chronology;
		} else if (diff < 50 * MINUTE_MILLIS) {
			return diff / MINUTE_MILLIS + (shortText ? "m" : " minutes " + chronology);
		} else if (diff < 90 * MINUTE_MILLIS) {
			return shortText ? "~1h" : "about an hour " + chronology;
		} else if (diff < 24 * HOUR_MILLIS) {
			return diff / HOUR_MILLIS + (shortText ? "h" : " hours " + chronology);
		} else if (diff < 48 * HOUR_MILLIS) {
			return shortText ? "~1d" : future ? "tomorrow" : "yesterday";
		} else if (diff < 14 * DAY_MILLIS || !shortText) {
			return diff / DAY_MILLIS + (shortText ? "d" : " days " + chronology);
		}
		return ">2w";
	}
}
