import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.voice.AudioProvider;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashMap;

public class CommandIniter {
    private AudioPlayerManager playerManager;
    private AudioPlayer player;
    private TrackScheduler scheduler;
    AudioProvider provider;

    public CommandIniter() {
        playerManager = new DefaultAudioPlayerManager();
        playerManager.getConfiguration()
                .setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        AudioSourceManagers.registerRemoteSources(playerManager);
        player = playerManager.createPlayer();
        provider = new LavaPlayerAudioProvider(player);
        scheduler = new TrackScheduler(player);
        playerManager.setItemLoaderThreadPoolSize(5);
    }

    public HashMap<String, Command> initBasicCommands() {
        HashMap<String, Command> commands = new HashMap<>();

        commands.put("ping", event -> event.getMessage().getChannel()
                .flatMap(channel -> channel.createMessage("Pong!"))
                .then());

        return commands;
    }

    public HashMap<String, Command> initAudioCommands() {
        HashMap<String, Command> commands = new HashMap<>();

        commands.put("join", event -> Mono.justOrEmpty(event.getMember())
                .flatMap(Member::getVoiceState)
                .flatMap(VoiceState::getChannel)
                // join returns a VoiceConnection which would be required if we were
                // adding disconnection features, but for now we are just ignoring it.
                .flatMap(channel -> channel.join(spec -> spec.setProvider(provider)))
                .then());

        commands.put("play", event -> Mono.justOrEmpty(event.getMessage().getContent())
                .map(content -> Arrays.asList(content.split(" ")))
                .doOnNext(command -> playerManager.loadItem(command.get(1), scheduler))
                .then());

        commands.put("leave", event -> Mono.justOrEmpty(event.getMember())
                .flatMap(Member::getVoiceState)
                .flatMap(VoiceState::getChannel)
                .flatMap(channel -> channel.join(spec -> spec.setProvider(provider)))
                .flatMap(connection -> connection.disconnect())
                .then());

        return commands;
    }
}