package testing;

import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Mono;

// basic command stuff
public interface Command {
    Mono<Void> execute(MessageCreateEvent event);
}