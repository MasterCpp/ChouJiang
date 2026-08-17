package com.jsys;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.Comparator;

public final class ChinaAccountMigrationIntegrationTest {
    private ChinaAccountMigrationIntegrationTest() {
    }

    public static void main(String[] args) throws Exception {
        chinaMigrationKeepsOwnerAndPublicActivityAvailable();
        branchAccountsAreIsolated();
        englishInstanceKeepsLegacyAuthenticationAndTsvStorage();
    }

    private static void branchAccountsAreIsolated() throws Exception {
        Path workDir = Files.createTempDirectory("jsys-account-isolation-");
        Process process = null;
        try {
            writeLegacyFixture(workDir.resolve("data"));
            int port = availablePort();
            process = startChineseInstance(workDir, port);
            waitForHealth(port);
            HttpClient client = HttpClient.newHttpClient();

            HttpResponse<String> first = post(client, port, "/api/admin/register", registration("US Team", "owner.us@example.com", "OwnerPass123"));
            assertEquals(201, first.statusCode(), "A Branch Account can self-register");
            String firstSession = first.headers().firstValue("set-cookie").orElseThrow(() -> new AssertionError("Missing first account session"));
            HttpResponse<String> second = post(client, port, "/api/admin/register", registration("Canada Team", "owner.ca@example.com", "OwnerPass123"));
            assertEquals(201, second.statusCode(), "A second Branch Account can self-register");
            String secondSession = second.headers().firstValue("set-cookie").orElseThrow(() -> new AssertionError("Missing second account session"));
            assertEquals(400, post(client, port, "/api/admin/register", registration("Duplicate Team", "owner.us@example.com", "OwnerPass123")).statusCode(),
                    "The exact same email can only register one Branch Account");
            assertEquals(201, post(client, port, "/api/admin/register", registration("Case-sensitive Team", "Owner.US@example.com", "OwnerPass123")).statusCode(),
                    "Email uniqueness preserves the agreed case-sensitive rule");

            HttpResponse<String> created = postWithCookie(client, port, "/api/admin/events", firstSession, eventForm("US-only event"));
            assertEquals(201, created.statusCode(), "First account can create an event in its workspace");
            String eventId = jsonString(created.body(), "id");

            HttpResponse<String> secondList = get(client, port, "/api/admin/events", secondSession);
            assertEquals(200, secondList.statusCode(), "Second account can load its own empty workspace");
            if (secondList.body().contains(eventId)) throw new AssertionError("Second account can see first account event");
            HttpResponse<String> forbiddenRead = get(client, port, "/api/admin/events/" + eventId, secondSession);
            assertEquals(404, forbiddenRead.statusCode(), "Second account cannot read a guessed event ID");

            HttpResponse<String> settings = get(client, port, "/api/admin/settings", firstSession);
            assertEquals(200, settings.statusCode(), "Owner can read account settings");
            assertContains(settings.body(), "owner.us@example.com", "Owner settings retain the immutable email");
            HttpResponse<String> changed = postWithCookie(client, port, "/api/admin/settings", firstSession,
                    form("workspaceName", "US Team Updated", "currentPassword", "OwnerPass123", "newPassword", "NewOwnerPass123"));
            assertEquals(200, changed.statusCode(), "Owner can update workspace name and password");
            assertContains(changed.body(), "US Team Updated", "Settings response contains the trimmed workspace name");
            assertEquals(401, get(client, port, "/api/admin/me", firstSession).statusCode(), "Changing password revokes the prior session");
            assertEquals(401, post(client, port, "/api/admin/login", "email=owner.us%40example.com&password=OwnerPass123").statusCode(), "Old Owner password no longer works");
            assertEquals(200, post(client, port, "/api/admin/login", "email=owner.us%40example.com&password=NewOwnerPass123").statusCode(), "New Owner password works");
        } finally {
            if (process != null) { process.destroyForcibly(); process.waitFor(); }
            deleteTree(workDir);
        }
    }

    private static void chinaMigrationKeepsOwnerAndPublicActivityAvailable() throws Exception {
        Path workDir = Files.createTempDirectory("jsys-china-migration-");
        Process process = null;
        try {
            writeLegacyFixture(workDir.resolve("data"));
            int port = availablePort();
            process = startChineseInstance(workDir, port);
            waitForHealth(port);

            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
            HttpResponse<String> login = post(client, port, "/api/admin/login", "email=china.owner%40example.com&password=ChinaPass123");
            assertEquals(200, login.statusCode(), "China Account Owner can sign in after migration");
            String session = login.headers().firstValue("set-cookie").orElseThrow(() -> new AssertionError("Missing session cookie"));

            HttpResponse<String> events = get(client, port, "/api/admin/events", session);
            assertEquals(200, events.statusCode(), "China Account Owner can list migrated activities");
            assertContains(events.body(), "legacy-event", "Migrated activity keeps its legacy ID");

            HttpResponse<String> logout = postWithCookie(client, port, "/api/admin/logout", session);
            assertEquals(200, logout.statusCode(), "China Account Owner can sign out");
            HttpResponse<String> afterLogout = get(client, port, "/api/admin/events", session);
            assertEquals(401, afterLogout.statusCode(), "Signed-out China Account Owner session is revoked immediately");

            HttpResponse<String> publicEvent = get(client, port, "/api/events/legacy-event", null);
            assertEquals(200, publicEvent.statusCode(), "Legacy public activity URL remains available");

            if (!Files.exists(workDir.resolve("data").resolve("jsys.db"))) {
                throw new AssertionError("SQLite database was not created during migration");
            }
        } finally {
            if (process != null) {
                process.destroyForcibly();
                process.waitFor();
            }
            deleteTree(workDir);
        }
    }

    private static void englishInstanceKeepsLegacyAuthenticationAndTsvStorage() throws Exception {
        Path workDir = Files.createTempDirectory("jsys-english-legacy-");
        Process process = null;
        try {
            writeLegacyFixture(workDir.resolve("data"));
            int port = availablePort();
            process = startEnglishInstance(workDir, port);
            waitForHealth(port);

            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
            HttpResponse<String> login = post(client, port, "/api/admin/login", "username=english-admin&password=EnglishPass123");
            assertEquals(200, login.statusCode(), "English instance keeps its independent legacy administrator login");
            if (Files.exists(workDir.resolve("data").resolve("jsys.db"))) {
                throw new AssertionError("English instance must not create the Chinese SQLite database");
            }
        } finally {
            if (process != null) {
                process.destroyForcibly();
                process.waitFor();
            }
            deleteTree(workDir);
        }
    }

    private static Process startChineseInstance(Path workDir, int port) throws IOException {
        ProcessBuilder builder = baseProcess(workDir, port);
        builder.environment().put("CHINA_ACCOUNT_EMAIL", "china.owner@example.com");
        builder.environment().put("CHINA_ACCOUNT_PASSWORD", "ChinaPass123");
        return builder.start();
    }

    private static Process startEnglishInstance(Path workDir, int port) throws IOException {
        ProcessBuilder builder = baseProcess(workDir, port);
        builder.environment().put("JSYS_LOCALE", "en");
        builder.environment().put("ADMIN_USERNAME", "english-admin");
        builder.environment().put("ADMIN_PASSWORD", "EnglishPass123");
        return builder.start();
    }

    private static ProcessBuilder baseProcess(Path workDir, int port) {
        String classpath = absoluteClasspath();
        ProcessBuilder builder = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", classpath,
                "com.jsys.App",
                Integer.toString(port)
        );
        builder.directory(workDir.toFile());
        builder.redirectErrorStream(true);
        return builder;
    }

    private static String absoluteClasspath() {
        String[] entries = System.getProperty("java.class.path").split(java.util.regex.Pattern.quote(File.pathSeparator));
        StringBuilder builder = new StringBuilder();
        for (String entry : entries) {
            if (!builder.isEmpty()) {
                builder.append(File.pathSeparator);
            }
            builder.append(Path.of(entry).toAbsolutePath().normalize());
        }
        return builder.toString();
    }

    private static void writeLegacyFixture(Path dataDir) throws IOException {
        Files.createDirectories(dataDir);
        Files.writeString(dataDir.resolve("events.tsv"), eventLine() + System.lineSeparator(), StandardCharsets.UTF_8);
        Files.writeString(dataDir.resolve("submissions.tsv"), "", StandardCharsets.UTF_8);
        Files.writeString(dataDir.resolve("winners.tsv"), "", StandardCharsets.UTF_8);
        Files.writeString(dataDir.resolve("operations.tsv"), "", StandardCharsets.UTF_8);
    }

    private static String eventLine() {
        return String.join("\t",
                encoded("legacy-event"),
                encoded("Legacy Activity"),
                encoded("Satisfaction"),
                encoded("Topic"),
                encoded("Alpha\nBeta"),
                encoded("Future topic"),
                encoded(""),
                encoded("Privacy"),
                "1",
                encoded("active"),
                encoded("2026-01-01T00:00:00Z"),
                encoded("2026-01-01T00:00:00Z")
        );
    }

    private static HttpResponse<String> post(HttpClient client, int port, String path, String body) throws IOException, InterruptedException {
        return client.send(HttpRequest.newBuilder(uri(port, path))
                .timeout(Duration.ofSeconds(2))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> get(HttpClient client, int port, String path, String cookie) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(port, path)).timeout(Duration.ofSeconds(2)).GET();
        if (cookie != null) {
            request.header("Cookie", cookie.split(";", 2)[0]);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> postWithCookie(HttpClient client, int port, String path, String cookie) throws IOException, InterruptedException {
        return client.send(HttpRequest.newBuilder(uri(port, path))
                .timeout(Duration.ofSeconds(2))
                .header("Cookie", cookie.split(";", 2)[0])
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> postWithCookie(HttpClient client, int port, String path, String cookie, String body) throws IOException, InterruptedException {
        return client.send(HttpRequest.newBuilder(uri(port, path)).timeout(Duration.ofSeconds(2))
                .header("Cookie", cookie.split(";", 2)[0]).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String registration(String workspaceName, String email, String password) {
        return form("workspaceName", workspaceName, "email", email, "password", password);
    }

    private static String eventForm(String title) {
        return form("title", title, "satisfactionQuestion", "Rate the event", "topicQuestion", "Topic", "topicOptions", "A\nB",
                "freeTextQuestion", "Future topic", "privacyNotice", "Privacy", "winningCount", "1", "status", "active");
    }

    private static String form(String... values) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < values.length; index += 2) {
            if (!builder.isEmpty()) builder.append('&');
            builder.append(URLEncoder.encode(values[index], StandardCharsets.UTF_8));
            builder.append('=').append(URLEncoder.encode(values[index + 1], StandardCharsets.UTF_8));
        }
        return builder.toString();
    }

    private static String jsonString(String body, String key) {
        String prefix = "\"" + key + "\":\"";
        int start = body.indexOf(prefix);
        if (start < 0) throw new AssertionError("Missing JSON key: " + key);
        int valueStart = start + prefix.length();
        int end = body.indexOf('"', valueStart);
        if (end < 0) throw new AssertionError("Unterminated JSON key: " + key);
        return body.substring(valueStart, end);
    }

    private static URI uri(int port, String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    private static void waitForHealth(int port) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        long deadline = System.nanoTime() + Duration.ofSeconds(8).toNanos();
        while (System.nanoTime() < deadline) {
            try {
                HttpResponse<String> response = get(client, port, "/api/health", null);
                if (response.statusCode() == 200) {
                    return;
                }
            } catch (IOException ignored) {
                Thread.sleep(100);
            }
        }
        throw new AssertionError("Chinese instance did not start");
    }

    private static int availablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static String encoded(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertContains(String value, String expected, String message) {
        if (!value.contains(expected)) {
            throw new AssertionError(message + ": missing " + expected);
        }
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException error) {
                    throw new RuntimeException(error);
                }
            });
        }
    }
}
