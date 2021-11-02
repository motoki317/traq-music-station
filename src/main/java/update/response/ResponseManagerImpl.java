package update.response;

import com.github.motoki317.traq_ws_bot.model.MessageCreatedEvent;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class ResponseManagerImpl implements ResponseManager {
    private final Map<UUID, List<Response>> waitingResponses;

    private final Object lock;

    public ResponseManagerImpl() {
        this.waitingResponses = new HashMap<>();
        this.lock = new Object();

        long delay = TimeUnit.MINUTES.toMillis(10);
        ResponseManagerImpl manager = this;
        new Timer().scheduleAtFixedRate(
                new TimerTask() {
                    @Override
                    public void run() {
                        manager.clearUp();
                    }
                },
                delay,
                delay
        );
    }

    @Override
    public void addEventListener(Response botResponse) {
        synchronized (this.lock) {
            UUID userId = botResponse.getUserId();
            if (!this.waitingResponses.containsKey(userId)) {
                this.waitingResponses.put(userId, new ArrayList<>());
            }

            this.waitingResponses.get(userId).add(botResponse);
        }
    }

    @Override
    public void handle(MessageCreatedEvent event) {
        UUID userId = UUID.fromString(event.message().user().id());

        synchronized (this.lock) {
            if (!this.waitingResponses.containsKey(userId)) {
                return;
            }

            UUID channelId = UUID.fromString(event.message().channelId());

            List<Response> responses = this.waitingResponses.get(userId);
            for (Iterator<Response> it = responses.iterator(); it.hasNext(); ) {
                Response r = it.next();

                if (r.getChannelId().equals(channelId)) {
                    boolean res = r.handle(event);
                    if (res) {
                        it.remove();
                        r.onDestroy();
                    }
                    break;
                }
            }

            if (responses.isEmpty()) {
                this.waitingResponses.remove(userId);
            }
        }
    }

    private void clearUp() {
        long now = System.currentTimeMillis();
        Predicate<Response> removeIf = r -> (now - r.getUpdatedAt()) > r.getMaxLive();

        synchronized (this.lock) {
            this.waitingResponses.values().stream()
                    .flatMap(Collection::stream)
                    .filter(removeIf)
                    .forEach(Response::onDestroy);
            this.waitingResponses.values().forEach(l -> l.removeIf(removeIf));
            this.waitingResponses.entrySet().removeIf(e -> e.getValue().isEmpty());
        }
    }
}
