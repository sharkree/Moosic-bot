import com.sedmelluq.discord.lavaplayer.player.*;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.audit.AuditLogEntry;
import discord4j.core.object.audit.AuditLogPart;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.AuditLogQueryFlux;
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
    private List<AuditLogEntry> entries = new ArrayList<discord4j.core.object.audit.AuditLogEntry>();

    // remember to not click on these(they will be replaced when an actual bot exists but right now it's sitting in my intellij projects folder)
    private static String PEPEGA_URL = "https://cdn.betterttv.net/emote/5aca62163e290877a25481ad/3x";
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
    }

    public HashMap<String, Command> initBasicCommands() {
        HashMap<String, Command> commands = new HashMap<>();

        commands.put("ping", event -> event.getMessage().getChannel()
                .flatMap(channel -> channel.createMessage(":ping_pong: **Pong!** " + (Instant.now().toEpochMilli() - event.getMessage().getTimestamp().toEpochMilli()) + "ms"))
                .then());

        commands.put("help", event -> event.getMessage().getChannel()
                .flatMap(channel -> channel.createMessage(EmbedCreateSpec.builder()
                        .author("Urban Lamp V2", RICKROLL_URL, PEPEGA_URL)
                        .title("Help! What are the commands again?")
                        .url(RICKROLL_URL)
                        .description("**IMPORTANT: The bot is in development, so please try to break it!(but don't ban admins/itself)**\n__**Command List:**__")
                        .addField("--Generic Commands--", ".", false)
                        .addField("!help", "The help command(how don't you know, you literally just did this!", false)
                        .addField("!ping", "returns latency of the bot", false)
                        .addField("--Music Commands--", ".", false)
                        .addField("!join", "Gets the bot to join a VC that you're in", false)
                        .addField("!play", "Gets the bot to play a youtube link", false)
                        .addField("!leave", "Tells the bot to leave the VC(since auto-leaving doesn't work", false)
                        .addField("--Moderation Commands--", ".", false)
                        .addField("!kick", "Kicks the pinged user", false)
                        .addField("!ban", "Bans the pinged user", false)
                        .addField("!unban", "Unbans the user(takes in the user's id, **important**", false)
                        .footer("!help called!", PEPEGA_URL)
                        .timestamp(Instant.now())
                        .build()))
                .then());

        return commands;
    }

    public HashMap<String, Command> initModerationCommands() {
        HashMap<String, Command> commands = new HashMap<>();

        // surely the users will actually ban someone...
        commands.put("kick", event -> {
            Guild guild = event.getGuild().block();
            try {
                assert guild != null;
                String msg = Arrays.asList(event.getMessage().getContent().split(" ")).get(1);
                assert msg != null;
                guild.kick(event.getMessage().getUserMentionIds().get(0), msg);
            } catch (Exception e) {
                Objects.requireNonNull(event.getMessage().getChannel().block()).createMessage(
                        EmbedCreateSpec.builder()
                                .title("Kick Unavailable")
                                .addField("", "Ban unavailable due to reason: " + e.getMessage(), false)
                                .timestamp(Instant.now())
                                .build());
            }

            return null;
        });

        commands.put("ban", event -> {
            Guild guild = event.getGuild().block();
            try {
                assert guild != null;
                guild.ban(event.getMessage().getUserMentionIds().get(0));
            } catch (Exception e) {
                Objects.requireNonNull(event.getMessage().getChannel().block()).createMessage(
                        EmbedCreateSpec.builder()
                                .title("Ban Unavailable")
                                .addField("", "Ban unavailable due to reason: " + e.getMessage(), false)
                                .timestamp(Instant.now())
                                .build());
            }

            return null;
        });

        commands.put("unban", new Command() {
            @Override
            public Mono<Void> execute(MessageCreateEvent event) {
                Guild guild = event.getGuild().block();
                assert guild != null;
                try {
                    guild.unban(Snowflake.of(Arrays.asList(event
                                            .getMessage()
                                            .getContent()
                                            .split(" "))
                                    .get(1)))
                            .then();
                } catch (Exception e) {
                    Objects.requireNonNull(event.getMessage().getChannel().block()).createMessage(
                            EmbedCreateSpec.builder()
                                    .title("Unban Unavailable")
                                    .addField("", "Could not find user id", false)
                                    .timestamp(Instant.now())
                                    .build());
                }

                return null;
            }
        });

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
                .flatMap(channel -> channel.join(spec -> spec.setProvider(provider)))
                .flatMap(VoiceConnection::disconnect)
                .then());

        return commands;
    }
}