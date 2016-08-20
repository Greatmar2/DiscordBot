package novaz.main;

import novaz.core.AbstractEventListener;
import novaz.db.model.OMusic;
import novaz.db.model.OServer;
import novaz.db.table.TServers;
import novaz.handler.*;
import novaz.handler.guildsettings.DefaultGuildSettings;
import novaz.handler.guildsettings.defaults.SettingActiveChannels;
import novaz.handler.guildsettings.defaults.SettingBotChannel;
import novaz.handler.guildsettings.defaults.SettingCommandPrefix;
import novaz.handler.guildsettings.defaults.SettingEnableChatBot;
import org.reflections.Reflections;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MessageBuilder;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;
import sx.blah.discord.util.audio.AudioPlayer;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NovaBot {

	public IDiscordClient instance;
	public CommandHandler commandHandler;
	public Timer timer = new Timer();
	private boolean isReady = false;
	private Map<IGuild, IChannel> defaultChannels = new ConcurrentHashMap<>();
	private ChatBotHandler chatBotHandler = null;
	public String mentionMe;


	public NovaBot() throws DiscordException {
		registerHandlers();
		instance = new ClientBuilder().withToken(Config.BOT_TOKEN).login();
		registerEvents();
	}

	/**
	 * check if a user is the owner of a guild or isCreator
	 *
	 * @param guild the server
	 * @param user  the user to check
	 * @return user is owner
	 */
	public boolean isOwner(IGuild guild, IUser user) {
		return guild.getOwner().equals(user) || isCreator(user);
	}

	/**
	 * checks if user is creator
	 *
	 * @param user user to check
	 * @return is creator?
	 */
	public boolean isCreator(IUser user) {
		return user.getID().equals(Config.CREATOR_ID);
	}

	/**
	 * Gets the default channel to output to
	 * if configured channel can't be found, return the first channel
	 *
	 * @param guild the guild to check
	 * @return default chat channel
	 */
	public IChannel getDefaultChannel(IGuild guild) {
		if (!defaultChannels.containsKey(guild)) {
			String channelName = GuildSettings.get(guild).getOrDefault(SettingBotChannel.class);
			List<IChannel> channelList = guild.getChannels();
			boolean foundChannel = false;
			for (IChannel channel : channelList) {
				if (channel.getName().equalsIgnoreCase(channelName)) {
					foundChannel = true;
					defaultChannels.put(guild, channel);
					break;
				}
			}
			if (!foundChannel) {
				defaultChannels.put(guild, channelList.get(0));
			}
		}
		return defaultChannels.get(guild);
	}

	public void markReady(boolean ready) {
		this.isReady = ready;
		setUserName(Config.BOT_NAME);
		loadConfiguration();
		mentionMe = "<@" + this.instance.getOurUser().getID() + ">";
		timer = new Timer();
	}

	public void loadConfiguration(IGuild guild) {

	}

	public void loadConfiguration() {
		commandHandler.load();
		TextHandler.getInstance().load();
		defaultChannels = new ConcurrentHashMap<>();
		chatBotHandler = new ChatBotHandler();
	}

	private void registerEvents() {
		Reflections reflections = new Reflections("novaz.event");
		Set<Class<? extends AbstractEventListener>> classes = reflections.getSubTypesOf(AbstractEventListener.class);
		for (Class<? extends AbstractEventListener> c : classes) {
			try {
				AbstractEventListener eventListener = c.getConstructor(NovaBot.class).newInstance(this);
				instance.getDispatcher().registerListener(eventListener);
			} catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
				e.printStackTrace();
			}
		}
	}

	public void addCustomCommand(IGuild server, String command, String output) {
		OServer serv = TServers.findBy(server.getID());
		commandHandler.addCustomCommand(serv.id, command, output);
	}

	public void removeCustomCommand(IGuild server, String command) {
		OServer serv = TServers.findBy(server.getID());
		commandHandler.removeCustomCommand(serv.id, command);
	}

	private void registerHandlers() {
		commandHandler = new CommandHandler(this);
	}

	public String getUserName() {
		return instance.getOurUser().getName();
	}

	public boolean setUserName(String newName) {
		if (isReady && !getUserName().equals(newName)) {
			try {
				instance.changeUsername(newName);
				return true;
			} catch (DiscordException | RateLimitException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	public void addSongToQueue(String filename, IGuild guild) {
		File file = new File(Config.MUSIC_DIRECTORY + filename); // Get file
		AudioPlayer player = AudioPlayer.getAudioPlayerForGuild(guild);
		try {
			player.queue(file);
		} catch (IOException | UnsupportedAudioFileException e) {
			e.printStackTrace();
		}
	}

	public void skipCurrentSong(IGuild guild) {
		MusicPlayerHandler.getAudioPlayerForGuild(guild, this).skipSong();
	}

	public void setVolume(IGuild guild, float vol) {
		AudioPlayer player = AudioPlayer.getAudioPlayerForGuild(guild);
		player.setVolume(vol);
	}

	public IMessage sendMessage(IChannel channel, String content) {
		try {
			return new MessageBuilder(instance).withChannel(channel).withContent(content).build();
		} catch (RateLimitException | DiscordException | MissingPermissionsException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void handleMessage(IGuild guild, IChannel channel, IUser author, IMessage message) {
		if (!isReady || author.isBot()) {
			return;
		}
		if (GuildSettings.get(guild).getOrDefault(SettingActiveChannels.class).equals("mine") &&
				!channel.getName().equalsIgnoreCase(GuildSettings.get(channel.getGuild()).getOrDefault(SettingBotChannel.class))) {
			return;
		}
		if (message.getContent().startsWith(GuildSettings.get(guild).getOrDefault(SettingCommandPrefix.class)) ||
				message.getContent().startsWith(mentionMe)) {
			commandHandler.process(guild, channel, author, message);
		} else if (
				Config.BOT_CHATTING_ENABLED.equals("true") &&
						GuildSettings.get(guild).getOrDefault(SettingEnableChatBot.class).equals("true") &&
						!DefaultGuildSettings.getDefault(SettingBotChannel.class).equals(GuildSettings.get(channel.getGuild()).getOrDefault(SettingBotChannel.class))
						&& channel.getName().equals(GuildSettings.get(channel.getGuild()).getOrDefault(SettingBotChannel.class))) {
			this.sendMessage(channel, this.chatBotHandler.chat(message.getContent()));
		}
	}

	public float getVolume(IGuild guild) {
		AudioPlayer player = AudioPlayer.getAudioPlayerForGuild(guild);
		return player.getVolume();
	}

	public void trackEnded(AudioPlayer.Track oldTrack, Optional<AudioPlayer.Track> nextTrack, IGuild guild) {
		MusicPlayerHandler.getAudioPlayerForGuild(guild, this).onTrackEnded(oldTrack, nextTrack);
	}

	public void trackStarted(AudioPlayer.Track track, IGuild guild) {
		MusicPlayerHandler.getAudioPlayerForGuild(guild, this).onTrackStarted(track);
	}

	public void stopMusic(IGuild guild) {
		MusicPlayerHandler.getAudioPlayerForGuild(guild, this).stopMusic();
	}

	public OMusic getCurrentlyPlayingSong(IGuild guild) {
		return MusicPlayerHandler.getAudioPlayerForGuild(guild, this).getCurrentlyPlaying();
	}

	public List<IUser> getCurrentlyListening(IGuild guild) {
		return MusicPlayerHandler.getAudioPlayerForGuild(guild, this).getUsersInVoiceChannel();
	}

	public boolean playRandomSong(IGuild guild) {
		return MusicPlayerHandler.getAudioPlayerForGuild(guild, this).playRandomSong();
	}
}