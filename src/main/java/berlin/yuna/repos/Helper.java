package berlin.yuna.repos;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Helper {

    public static final Map<String, LinkedHashMap<String, String>> REPO_MAP = new LinkedHashMap<>();
    public static final String PROJECT_DIR = System.getProperty("user.dir");
    private static final String GITHUB_TOKEN = System.getenv("GITHUB_TOKEN");

    public static String asUrl(final String name, final String url) {
        return "[" + name + "](" + url + ")";
    }

    public static JsonObject getObject(final String url) throws IOException {
        return getJson(url).readObject();
    }

    public static JsonArray getArray(final String url) throws IOException {
        return getJson(url).readArray();
    }

    private static JsonReader getJson(final String url) throws IOException {
        final HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        if (GITHUB_TOKEN != null && GITHUB_TOKEN.length() > 5 && url.contains("github.com")) {
            con.setRequestProperty("Authorization", "token " + GITHUB_TOKEN);
        }
        con.setRequestMethod("GET");
        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);
        return Json.createReader(new InputStreamReader((con.getInputStream())));
    }

    public static String get(final String url) throws IOException {
        return get(url, null);
    }

    public static String get(final String url, final Map<String, String> header) throws IOException {
        final StringBuilder result = new StringBuilder();
        final HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);
        (header == null ? new HashMap<String, String>() : header).forEach(con::setRequestProperty);

        final BufferedReader br = new BufferedReader(new InputStreamReader((con.getInputStream())));

        String output;
        while ((output = br.readLine()) != null) {
            result.append(output);
        }
        return result.toString();
    }

    public static JsonArray toArray(final HttpURLConnection connection) throws IOException {
        return toArray(connection.getInputStream());
    }

    public static JsonArray toArray(final InputStream inputStream) {
        return Json.createReader(inputStream).readArray();
    }

    public static JsonObject toObject(final HttpURLConnection connection) throws IOException {
        return toObject(connection.getInputStream());
    }

    public static JsonObject toObject(final InputStream inputStream) {
        return Json.createReader(inputStream).readObject();
    }

    public static JsonValue toValue(final HttpURLConnection connection) throws IOException {
        return toValue(connection.getInputStream());
    }

    public static JsonValue toValue(final InputStream inputStream) {
        return Json.createReader(inputStream).readValue();
    }
}
