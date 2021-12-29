import com.sedmelluq.discord.lavaplayer.player.*;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.voice.*;
import reactor.core.publisher.Mono;
import java.time.Instant;
import java.util.*;

public class CommandIniter {
    private AudioPlayerManager playerManager;
    private AudioPlayer player;
    private TrackScheduler scheduler;
    private AudioProvider provider;
    private List<String> queue;

    // remember to not click on these(they will be replaced when an actual bot exists but right now it's sitting in my intellij projects folder)
    private static String KEK_URL = "https://cdn.betterttv.net/emote/5aca62163e290877a25481ad/3x";
    private static String RICKROLL_URL = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";

    public CommandIniter() {
        playerManager = new DefaultAudioPlayerManager();
        playerManager.getConfiguration()
                .setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        AudioSourceManagers.registerRemoteSources(playerManager);
        player = playerManager.createPlayer();
        provider = new LavaPlayerAudioProvider(player);
        scheduler = new TrackScheduler(player);
        playerManager.setItemLoaderThreadPoolSize(5);

        queue = new ArrayList<>();
    }

    public HashMap<String, Command> initBasicCommands() {
        HashMap<String, Command> commands = new HashMap<>();

        commands.put("ping", event -> event.getMessage().getChannel()
                .flatMap(channel -> channel.createMessage(":ping_pong: Pong! " + (Instant.now().toEpochMilli() - event.getMessage().getTimestamp().toEpochMilli()) + "ms"))
                .then());

        commands.put("help", event -> event.getMessage().getChannel()
                .flatMap(channel -> channel.createMessage(EmbedCreateSpec.builder()
                        .author("Urban Lamp V2", RICKROLL_URL, KEK_URL)
                        .title("!Help")
                        .url(RICKROLL_URL)
                        .description("bot isn't done yet,\nso please don't call this command\n lmao")
                        .addField("deez nuts", "there is no help", true)
                        .footer("!help", KEK_URL)
                        .timestamp(Instant.now())
                        .build()))
                .then());

        commands.put("uwu", event -> event.getMessage().getChannel()
                .flatMap(channel -> channel.createMessage("you have joined the uwu cult. dm chry for more details"))
                .then());

        return commands;
    }

    public HashMap<String, Command> initAudioCommands() {
        HashMap<String, Command> commands = new HashMap<>();

        commands.put("join", event -> Mono.justOrEmpty(event.getMember())
                .flatMap(Member::getVoiceState)
                .flatMap(VoiceState::getChannel)
                // "join returns a VoiceConnection which would be required if we are adding disconnection features, but for now we are just ignoring it."
                // yes I forgot to do that :clown:
                .flatMap(channel -> channel.join(spec -> spec.setProvider(provider)))
                .then());

        commands.put("play", event -> Mono.justOrEmpty(event.getMessage().getContent())
                .map(content -> Arrays.asList(content.split(" ")))
                .doOnNext(command -> playerManager.loadItem(command.get(1), scheduler))
                .then());

        commands.put("leave", event -> Mono.justOrEmpty(event.getMember())
                .flatMap(Member::getVoiceState)
                .flatMap(VoiceState::getChannel)
                // monke stuff since I messed up on the join command
                // joining and leaving too good
                .flatMap(channel -> channel.join(spec -> spec.setProvider(provider)))
                .flatMap(VoiceConnection::disconnect)
                .then());

        return commands;
    }
}