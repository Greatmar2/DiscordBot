package discordbot.util;

import discordbot.main.Config;
import discordbot.permission.SimpleRank;
import org.apache.commons.lang3.StringEscapeUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YTUtil {
	private final static Pattern youtubeCode = Pattern.compile("^[A-Za-z0-9_-]{11}$");
	private final static Pattern yturl = Pattern.compile("^.*((youtu.be/)|(v/)|(/u/\\w/)|(embed/)|(watch\\?))\\\\??v?=?([^#\\\\&\\?]*).*");

	/**
	 * checks if it could be a youtube videocode
	 *
	 * @param videocode code to check
	 * @return could be a code
	 */
	public static boolean isValidYoutubeCode(String videocode) {
		return youtubeCode.matcher(videocode).matches();
	}

	/**
	 * Extracts the videocode from an url
	 *
	 * @param url youtube link
	 * @return videocode
	 */
	public static String extractCodeFromUrl(String url) {
		Matcher matcher = yturl.matcher(url);
		if (matcher.find()) {
			return matcher.group(7);
		}
		return url;
	}

	/**
	 * @param videocode youtubecode
	 * @return whats in the <title> tag on a youtube page
	 */
	public static String getTitleFromPage(String videocode) {
		String ret = "";
		try {
			URL loginurl = new URL("https://www.youtube.com/watch?v=" + videocode);
			URLConnection yc = loginurl.openConnection();
			yc.setConnectTimeout(10 * 1000);
			BufferedReader in = new BufferedReader(
					new InputStreamReader(
							yc.getInputStream()));
			String input = "";
			String inputLine;
			while ((inputLine = in.readLine()) != null)
				input += inputLine;
			in.close();
			int start = input.indexOf("<title>");
			int end = input.indexOf("</title>");
			ret = input.substring(start + 7, end - 10);
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		}
		return StringEscapeUtils.unescapeHtml4(ret);
	}

	/**
	 * downloads a youtube video as an mp3
	 *
	 * @param videocode youtube video id
	 * @param userRank
	 * @return success or not
	 */
	public static boolean downloadfromYoutubeAsMp3(String videocode, SimpleRank userRank) {
		System.out.println("YT:: downloading " + videocode);
		System.out.println("YT:: https://www.youtube.com/watch?v=" + videocode);
		List<String> infoArgs = new LinkedList<>();
		infoArgs.add(Config.YOUTUBEDL_EXE);
		infoArgs.add("--verbose");
		infoArgs.add("--no-check-certificate");
		infoArgs.add("-x"); //audio only
		infoArgs.add("--ffmpeg-location");
		infoArgs.add(Config.YOUTUBEDL_BIN);
		infoArgs.add("--audio-format");
		infoArgs.add("mp3");
		infoArgs.add("--max-filesize");
		infoArgs.add(userRank.isAtLeast(SimpleRank.CONTRIBUTOR) ? "128m" : "32m");
		infoArgs.add("--output");
		infoArgs.add(Config.MUSIC_DIRECTORY + "/" + videocode + ".%(ext)s");
		infoArgs.add("https://www.youtube.com/watch?v=" + videocode);
		ProcessBuilder builder = new ProcessBuilder().command(infoArgs);
		builder.redirectErrorStream(true);
		Process process = null;
		try {
			process = builder.start();
			InputStream stdout = process.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
			String line;
//			while ((line = reader.readLine()) != null) {
//				System.out.println("YT: " + line);
//			}
			process.waitFor();
			process.destroy();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public static String getOutputPath(String videoCode) {
		return Config.MUSIC_DIRECTORY + videoCode + ".mp3";
	}

	public static boolean downloadPlayList(String playlist) {
		System.out.println("YT:: downloading " + playlist);
		List<String> infoArgs = new LinkedList<>();
		infoArgs.add(Config.YOUTUBEDL_EXE);
		infoArgs.add("--verbose");
		infoArgs.add("--no-check-certificate");
		infoArgs.add("-x"); //audio only
		infoArgs.add("--ignore-errors"); //audio only
		infoArgs.add("--prefer-avconv");
		infoArgs.add("--ffmpeg-location");
		infoArgs.add(Config.YOUTUBEDL_BIN);
		infoArgs.add("--audio-format");
		infoArgs.add("mp3");
		infoArgs.add("--output");
		infoArgs.add(Config.MUSIC_DIRECTORY + "tmp/%(id)s.%(ext)s");
		infoArgs.add(playlist);
		ProcessBuilder builder = new ProcessBuilder().command(infoArgs);
		builder.redirectErrorStream(true);
		Process process = null;
		try {
			process = builder.start();
			InputStream stdout = process.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
			String line = "";
			while ((line = reader.readLine()) != null) {
				System.out.println("YT: " + line);
			}
			process.waitFor();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
