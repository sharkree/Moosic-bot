import discord4j.core.*;
import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Bot {
    private static Map<String, Command> commands = new HashMap<>();

    public static void main(String[] args) throws IOException  {
        final String BOT_PREFIX = "!";
        final String TOKEN = (new BufferedReader(new FileReader("token.txt"))).readLine();

        CommandIniter initer = new CommandIniter();
        commands.putAll(initer.initBasicCommands());
        commands.putAll(initer.initAudioCommands());

        final GatewayDiscordClient client = DiscordClientBuilder.create(TOKEN).build()
                .login()
                .block();

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