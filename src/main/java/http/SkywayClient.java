package http;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class SkywayClient {
    private static final String apiKey = System.getenv("SKYWAY_APIKEY");
    private static final String fileContent;

    static {
        File file = new File("client.html");
        byte[] data;
        try {
            FileInputStream fis = new FileInputStream(file);
            data = new byte[(int) file.length()];
            IOUtils.readFully(fis, data);
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new Error("error reading file: " + e.getMessage());
        }

        fileContent = new String(data, StandardCharsets.UTF_8);
    }

    static String getHtml(UUID vcId, String roomName, String peerId, int port) {
        return String.format(fileContent,
                peerId,
                apiKey,
                roomName,
                port,
                vcId.toString(),
                port
        );
    }
}
