package de.maxhenkel.audioplayer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

public class Filebin {

    public static void downloadSound(MinecraftServer server, UUID sound) throws IOException, InterruptedException, UnsupportedAudioFileException {
        String url = getBin(sound);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException(url + " responded with status " + response.statusCode());
        }

        JsonParser parser = new JsonParser();
        JsonElement json = parser.parse(response.body());

        if (!(json instanceof JsonObject object)) {
            throw new IOException("Invalid response");
        }

        JsonElement filesElement = object.get("files");

        if (filesElement == null) {
            throw new IOException("No files uploaded");
        }

        if (!(filesElement instanceof JsonArray files)) {
            throw new IOException("No files uploaded");
        }

        for (JsonElement element : files) {
            if (!(element instanceof JsonObject file)) {
                continue;
            }

            String contentType = file.get("content-type").getAsString();

            if (contentType.equals("audio/wav")) {
                String filename = file.get("filename").getAsString();
                AudioManager.saveSound(server, sound, url + "/" + filename);
                return;
            }
        }
        throw new IOException("No wav files uploaded");
    }

    public static String getBin(UUID sound) {
        String filebinUrl = AudioPlayer.SERVER_CONFIG.filebinUrl.get();

        if (!filebinUrl.endsWith("/")) {
            filebinUrl += "/";
        }

        return filebinUrl + sound;
    }

}
