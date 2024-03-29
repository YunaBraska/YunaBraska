package berlin.yuna.repos;


import javax.json.JsonValue;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static berlin.yuna.repos.Helper.PROJECT_DIR;
import static berlin.yuna.repos.Helper.REPO_MAP;
import static berlin.yuna.repos.Helper.asUrl;
import static berlin.yuna.repos.Helper.getArray;
import static java.lang.System.lineSeparator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;


public class Main {

    private static final String LINE_SEPARATOR = lineSeparator();

//    private static Optional<HttpURLConnection> httpRequest(final String requestUrl, final String method) {
//        try {
//            final URL url = new URL(requestUrl);
//            final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//            conn.setRequestMethod(method);
//            conn.setRequestProperty("Range", "bytes=0-0");
//            conn.setRequestProperty("user-agent", "Europace/unterlagenakte");
//
//            final var response = Optional.of(conn);
//            conn.disconnect();
//
//            return response;
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return Optional.empty();
//    }

    //TODO:
    // gitignore
    public static void main(final String[] args) throws IOException {

//        for (final String url : new String[]{
//                "https://webhook.site/132599b9-c80c-4fcf-999b-f42b090ffa7a",
//                "https://picsum.photos/536/354",
//                "https://picsum.photos/id/237/200/300",
//                "https://picsum.photos/seed/picsum/200/300",
//                "https://play-lh.googleusercontent.com/ahJtMe0vfOlAu1XJVQ6rcaGrQBgtrEZQefHy7SXB7jpijKhu1Kkox90XDuH8RmcBOXNn",
//                "https://upload.wikimedia.org/wikipedia/commons/thumb/2/21/ZDF_logo%21_Logo_2021.svg/640px-ZDF_logo%21_Logo_2021.svg.png"
//        }) {
//            final HttpURLConnection connection = httpRequest(url, "GET").get();
//            final Integer fileSize = Optional.ofNullable(connection.getHeaderField("Content-Range"))
//                    .filter(Objects::nonNull)
//                    .map(s -> s.replace("bytes 0-0/", ""))
//                    .filter(s -> !s.equals("1"))
//                    .map(Integer::parseInt).orElseGet(connection::getContentLength);
//
//            System.out.println("__________________________________________________________________");
//            System.out.println("URL            : " + connection.getURL());
//            System.out.println("Content-Type   : " + connection.getContentType());
//            System.out.println("Content-Length : " + connection.getContentLength());
//            System.out.println("Content-Range  : " + connection.getHeaderField("Content-Range"));
//            System.out.println("Size           : " + fileSize);
//            System.out.println("------------------------------------------------------------------");
//        }
//

        System.exit(0);
        REPO_MAP.clear();
        final var repos = getArray("https://api.github.com/users/YunaBraska/repos");
        for (JsonValue repoRaw : repos) {
            final var repo = repoRaw.asJsonObject();
            final var tags = getArray(repo.getString("tags_url"));
            final var name = repo.getString("name");
            final var html_url = repo.getString("html_url");
            final var repoId = repo.getString("full_name");
            if (!tags.isEmpty() && !repo.getBoolean("archived") && !repo.getBoolean("disabled") && !"YunaBraska".equalsIgnoreCase(repo.getString("name", ""))) {
                System.out.println("Updating [" + name + "]");
                addRow(name, "Name", name, html_url);
                addRow(name, "Tag", tags.iterator().next().asJsonObject().getString("name"), html_url + "/tags");
                addRow(name, "Updated", ZonedDateTime.parse(repo.getString("updated_at"), DateTimeFormatter.ISO_ZONED_DATE_TIME).format(DateTimeFormatter.ISO_LOCAL_DATE));
                addRow(name, "Stars", repo.getJsonNumber("stargazers_count").intValue(), html_url + "/stargazers");
                addRow(name, "Issues", repo.getJsonNumber("open_issues_count").intValue(), html_url + "/issues");
                addRow(name, "Size", repo.getJsonNumber("size").intValue());
                addRow(name, "Maintainability", "![maintainability](https://img.shields.io/codeclimate/maintainability/" + repoId + "?style=flat-square)");
                addRow(name, "Coverage", "![coverage](https://img.shields.io/codeclimate/coverage/" + repoId + "?style=flat-square)");
                addRow(name, "Description", repo.getString("description", ""));
            } else {
                System.out.println("Skipped [" + name + "]");
            }
        }
        writeReadmeFile();
    }

    private static void writeReadmeFile() throws IOException {
        final var result = new StringBuilder();
        final var sortedMap = new LinkedHashMap<String, LinkedHashMap<String, String>>();
//        REPO_MAP.entrySet().stream().sorted(Map.Entry.comparingByValue((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o2.get("Updated"), o1.get("Updated")))).forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));
        REPO_MAP.entrySet().stream().sorted(Map.Entry.comparingByValue((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.get("Name"), o2.get("Name")))).forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));
        result
                .append("[![YunaBraska's GitHub stats](https://github-readme-stats.vercel.app/api?username=YunaBraska&count_private=true&show_icons=true&theme=dracula)](https://github.com/YunaBraska/github-readme-stats)")
                .append(LINE_SEPARATOR)
                .append(LINE_SEPARATOR)
                //TABLE HEAD START
                .append(sortedMap.values().iterator().next().keySet().stream().collect(joining("|", "|", "|")))
                .append(LINE_SEPARATOR)
                //TABLE HEAD END
                .append(sortedMap.values().iterator().next().keySet().stream().map(s -> "---").collect(joining("|", "|", "|")))
                .append(LINE_SEPARATOR);
        //TABLE CONTENT START
        sortedMap.forEach((row, column) -> result.append(column.values().stream().collect(joining("|", "|", "|" + LINE_SEPARATOR))));
        //TABLE CONTENT END
        result
                .append(LINE_SEPARATOR)
                .append("Last update [").append(ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE)).append("]")
                .append(LINE_SEPARATOR);
        Files.write(Path.of(PROJECT_DIR, "README.md"), result.toString().getBytes(UTF_8));
    }

    public static void addRow(final String repo, final String column, final Object value) {
        addRow(repo, column, value, null);
    }

    public static void addRow(final String repo, final String column, final Object value, final String url) {
        final String val = (value instanceof String) ? (String) value : String.valueOf(value);
        REPO_MAP.computeIfAbsent(repo, table -> new LinkedHashMap<>()).put(column, url != null ? asUrl(val, url) : val);
    }
}
