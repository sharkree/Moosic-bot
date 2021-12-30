package testing;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.voice.AudioProvider;
import discord4j.voice.VoiceConnection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static final AudioPlayerManager PLAYER_MANAGER;
    public static VoiceChannel channel;
    public static Map<String, Command> commands = new HashMap<>();
    public static TrackScheduler scheduler;
    public static VoiceConnection connection;

    static {
        PLAYER_MANAGER = new DefaultAudioPlayerManager();
        // This is an optimization strategy that Discord4J can utilize to minimize allocations
        PLAYER_MANAGER.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        AudioSourceManagers.registerRemoteSources(PLAYER_MANAGER);
        AudioSourceManagers.registerLocalSource(PLAYER_MANAGER);
        scheduler = new TrackScheduler(PLAYER_MANAGER.createPlayer());
    }

    public static void main(String[] args) throws IOException  {
        commands.put("play", new Command() {
            @Override
            public Mono<Void> execute(MessageCreateEvent event) {
                channel = Mono.justOrEmpty(event.getMember())
                        .flatMap(Member::getVoiceState)
                        .flatMap(VoiceState::getChannel).block();
                assert channel != null;
                final AudioProvider provider = GuildAudioManager.of(channel.getGuildId()).getProvider();
                connection = channel.join(spec -> spec.setProvider(provider)).block();
                final List<String> args = Arrays.asList(event.getMessage().getContent().split(" "));
                PLAYER_MANAGER.loadItem(args.get(1), scheduler);
                return null;
            }
        });

        // TODO: make bot prefix configurable?
        final String BOT_PREFIX = "!";
        // TODO: y isn't this global lmao
        final String TOKEN = (new BufferedReader(new FileReader("token.txt"))).readLine(); // please java shut up

        final GatewayDiscordClient client = DiscordClientBuilder.create(TOKEN).build()
                .login()
                .block();

        // pls Intellij nobody cares about a null reference if they already made it
        assert client != null;
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .flatMap(event -> Mono.just(event.getMessage().getContent())
                        .flatMap(content -> Flux.fromIterable(commands.entrySet())
                                .filter(entry -> content.startsWith(BOT_PREFIX + entry.getKey()))
                                .flatMap(entry -> entry.getValue().execute(event))
                                .next()))
                .subscribe();

        client.onDisconnect().block();
    }
}