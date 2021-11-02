package http;

import api.TraqApi;
import app.App;
import com.github.motoki317.traq4j.model.WebRTCAuthenticateResult;
import com.github.motoki317.traq4j.model.WebRTCUserState;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import log.Logger;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MusicServerImpl implements MusicServer {
    private final Pattern servePath;
    private final Pattern skywayPath;
    private final Logger logger;
    private final TraqApi traqApi;
    private final int port;
    private final String botUserId;

    // audio players waiting for a http connection
    private final Map<UUID, AudioPlayer> waitingPlayers;
    private final Map<UUID, MusicServerAudioSender> activeSenders;

    public MusicServerImpl(int port, App app) throws IOException {
        this.servePath = Pattern.compile("/serve/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})");
        this.skywayPath = Pattern.compile("/skyway/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})");
        this.logger = app.getLogger();
        this.traqApi = app.getTraqApi();
        this.port = port;
        this.botUserId = System.getenv("BOT_USER_ID");
        this.waitingPlayers = new HashMap<>();
        this.activeSenders = new HashMap<>();

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::top);
        server.createContext("/serve/", this::serveMusic);
        server.createContext("/skyway/", this::serveSkyway);
        server.createContext("/authenticate", this::authenticate);
        server.start();
        this.logger.log("Music server started on port " + port + "...");
    }

    private static void status(HttpExchange exchange, int code) throws IOException {
        exchange.sendResponseHeaders(code, 0);
    }

    private static void respond(HttpExchange exchange, String res) throws IOException {
        OutputStream out = exchange.getResponseBody();
        out.write(res.getBytes());
    }

    private static void flushAndClose(HttpExchange exchange) throws IOException {
        OutputStream out = exchange.getResponseBody();
        out.flush();
        out.close();
    }

    public void top(HttpExchange exchange) throws IOException {
        status(exchange, 200);
        respond(exchange, "Music server successfully started!");
        flushAndClose(exchange);
    }

    public synchronized void serveMusic(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        Matcher m = this.servePath.matcher(path);
        if (!m.matches()) {
            status(exchange, 400);
            respond(exchange, "bad path");
            flushAndClose(exchange);
            return;
        }

        UUID vcId = UUID.fromString(m.group(1));
        AudioPlayer player = this.waitingPlayers.getOrDefault(vcId, null);
        if (player == null) {
            this.logger.log("[Music server] Cannot find player for channel id " + vcId);
            status(exchange, 400);
            respond(exchange, "no waiting players for channel id " + vcId + " found");
            flushAndClose(exchange);
            return;
        }

        Headers headers = exchange.getResponseHeaders();
        headers.add("Cache-Control", "no-cache, no-store");
        headers.add("Content-Type", "audio/ogg;codecs=opus");
        headers.add("Connection", "close");

        status(exchange, 200);
        if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
            flushAndClose(exchange);
            return;
        }

        OutputStream out = exchange.getResponseBody();

        this.waitingPlayers.remove(vcId);
        this.activeSenders.put(vcId, new MusicServerAudioSender(player, out));
        this.logger.log("[Music server] Connection established to vc id " + vcId);
    }

    private synchronized void serveSkyway(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        Matcher m = this.skywayPath.matcher(path);
        if (!m.matches()) {
            status(exchange, 400);
            respond(exchange, "bad path");
            flushAndClose(exchange);
            return;
        }

        UUID vcId = UUID.fromString(m.group(1));
        List<WebRTCUserState> states = this.traqApi.getWebRTCState();
        if (states == null) {
            status(exchange, 500);
            respond(exchange, "failed to get WebRTC states");
            flushAndClose(exchange);
            return;
        }

        String roomName = null;
        for (WebRTCUserState state : states) {
            if (!state.getChannelId().equals(vcId)) continue;
            if (state.getSessions().size() == 0) continue;
            roomName = state.getSessions().get(0).getSessionId();
            break;
        }
        if (roomName == null) {
            status(exchange, 400);
            respond(exchange, "qall not going on at channel " + vcId);
            flushAndClose(exchange);
            return;
        }

        Headers headers = exchange.getResponseHeaders();
        headers.add("Content-Type", "text/html");
        status(exchange, 200);
        respond(exchange, SkywayClient.getHtml(vcId, roomName, botUserId, port));
        flushAndClose(exchange);

        this.logger.log("[Music server] Skyway: Connecting to room " + roomName + "...");
    }

    private void authenticate(HttpExchange exchange) throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(exchange.getRequestBody(), writer, "UTF-8");
        String peerId = (String) new JSONObject(writer.toString()).get("peerId");

        WebRTCAuthenticateResult auth = this.traqApi.authenticateWebRTC(peerId);
        if (auth == null) {
            this.logger.log("[Music server] Failed to authenticate WebRTC");
            status(exchange, 500);
            respond(exchange, "Failed to authenticate");
            flushAndClose(exchange);
            return;
        }

        JSONObject response = new JSONObject()
                .put("peerId", auth.getPeerId())
                .put("authToken", auth.getAuthToken())
                .put("ttl", auth.getTtl())
                .put("timestamp", auth.getTimestamp());

        Headers headers = exchange.getResponseHeaders();
        headers.add("Content-Type", "application/json");

        this.logger.log("[Music server] authenticated WebRTC token");
        status(exchange, 200);
        respond(exchange, response.toString());
        flushAndClose(exchange);
    }

    @Override
    public synchronized String serve(String peerId, AudioPlayer player, UUID channelId) {
        this.waitingPlayers.put(channelId, player);
        this.logger.log("[Music server] Waiting access to vc id " + channelId.toString() + "...");

        // connect to this next
        return "http://localhost:" + port + "/skyway/" + channelId;
    }

    @Override
    public synchronized void stop(UUID channelId) {
        MusicServerAudioSender sender = this.activeSenders.getOrDefault(channelId, null);
        if (sender == null) {
            this.logger.log("[Music server] Warning: tried to stop non-existing sender: " + channelId.toString());
            return;
        }
        sender.shutdown();
    }
}
