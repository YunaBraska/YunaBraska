package berlin.yuna.repos;


import javax.json.JsonValue;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import static berlin.yuna.repos.Helper.PROJECT_DIR;
import static berlin.yuna.repos.Helper.REPO_MAP;
import static berlin.yuna.repos.Helper.asUrl;
import static berlin.yuna.repos.Helper.getArray;
import static java.lang.System.lineSeparator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;


public class Main {

    //TODO:
    // mvn wrapper
    // mvn dependency update
    // mvn maven central template
    // gitignore
    // git workflow template
    public static void main(String[] args) throws IOException {
        final var result = new StringBuilder();
        result.append("[![YunaBraska's GitHub stats](https://github-readme-stats.vercel.app/api?username=YunaBraska&count_private=true&show_icons=true&theme=dracula)](https://github.com/YunaBraska/github-readme-stats)").append(lineSeparator()).append(lineSeparator());
        REPO_MAP.clear();
        final var repos = getArray("https://api.github.com/users/YunaBraska/repos");
        for (JsonValue repoRaw : repos) {
            final var repo = repoRaw.asJsonObject();
            final var tags = getArray(repo.getString("tags_url"));
            final var name = repo.getString("name");
            final var html_url = repo.getString("html_url");
            final var repoId = repo.getString("full_name");
            if (!tags.isEmpty()) {
                addRow(name, "Name", name, html_url);
                addRow(name, "Tag", tags.iterator().next().asJsonObject().getString("name"), html_url + "/tags");
                addRow(name, "Updated", ZonedDateTime.parse(repo.getString("updated_at"), DateTimeFormatter.ISO_ZONED_DATE_TIME).format(DateTimeFormatter.ISO_LOCAL_DATE));
                addRow(name, "Stars", repo.getJsonNumber("stargazers_count").intValue(), html_url + "/stargazers");
                addRow(name, "Issues", repo.getJsonNumber("open_issues_count").intValue(), html_url + "/issues");
                addRow(name, "Size", repo.getJsonNumber("size").intValue());
                addRow(name, "Maintainability", "![maintainability](https://img.shields.io/codeclimate/maintainability/" + repoId + "?style=flat-square)");
                addRow(name, "Coverage", "![coverage](https://img.shields.io/codeclimate/coverage/" + repoId + "?style=flat-square)");
                addRow(name, "Description", repo.getString("description"));
            }
        }
        LinkedHashMap<String, LinkedHashMap<String, String>> sortedMap = new LinkedHashMap<>();
        REPO_MAP.entrySet().stream().sorted(Map.Entry.comparingByValue((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o2.get("Updated"), o1.get("Updated")))).forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));
        result.append(sortedMap.values().iterator().next().keySet().stream().collect(joining("|", "|", "|"))).append(lineSeparator());
        result.append(sortedMap.values().iterator().next().keySet().stream().map(s -> "---").collect(joining("|", "|", "|"))).append(lineSeparator());
        sortedMap.forEach((row, column) -> result.append(column.values().stream().collect(joining("|", "|", "|"))).append(lineSeparator()));
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
