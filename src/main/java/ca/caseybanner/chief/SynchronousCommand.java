package ca.caseybanner.chief;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;

/**
 * Created by Chris on 10/14/14.
 */
public abstract class SynchronousCommand extends Command {
    /**
     * Constructor
     *
     * @param bot the Bot this command belongs to
     */
    protected SynchronousCommand(Bot bot) {
        super(bot);
    }

    /**
     * Process a message and optionally return a response.
     *
     * @param from the nickname the chat was from
     * @param message the message itself
     * @param matcher the matcher created by pattern returned by getPattern(),
     *	               already run on the message
     * @param fromRoom whether or not this message come from a room
     * @return optional response to the message
     */
    public abstract Optional<String> processMessage(
            String from, String message, Matcher matcher, boolean fromRoom);

    /**
     * Possibly async processing
     *
     * @param from
     * @param message
     * @param matcher
     * @param fromRoom
     * @return
     */
    public CompletableFuture<Optional<String>> processAsyncMessage(
            String from, String message, Matcher matcher, boolean fromRoom) {
        return toFuture(processMessage(from, message, matcher, fromRoom));
    }
}
