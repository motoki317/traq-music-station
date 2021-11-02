package app;

import com.github.motoki317.traq4j.ApiException;

/**
 * Message respond helper for bot.
 * Created on every received event to match for the event channel.
 */
public interface Responder {
    /**
     * Responds to the channel on which message was received.
     * @param content Content of the message.
     * @param embed If {@code true}, then automatically converts embeds in the given message.
     *             (i.e. adds 'embed=1' in the request param)
     * @throws ApiException on request failure.
     */
    void respond(String content, boolean embed) throws ApiException;
}
