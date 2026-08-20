package com.jsys;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class App {
    private static final int DEFAULT_PORT = 8080;
    private static final int HTTP_WORKER_COUNT = 16;
    private static final int HTTP_QUEUE_CAPACITY = 256;
    private static final Map<String, Instant> SESSIONS = new ConcurrentHashMap<>();

    private App() {
    }

    public static void main(String[] args) throws IOException {
        if ("en".equalsIgnoreCase(System.getenv("JSYS_LOCALE"))) {
            startLegacyInstance(args);
            return;
        }

        startChineseInstance(args);
    }

    private static void startLegacyInstance(String[] args) throws IOException {
        int port = resolvePort(args);
        Path webRoot = Path.of("frontend", "public").toAbsolutePath().normalize();
        EventStore eventStore = new EventStore(Path.of("data", "events.tsv"));
        SubmissionStore submissionStore = new SubmissionStore(Path.of("data", "submissions.tsv"));
        WinnerStore winnerStore = new WinnerStore(Path.of("data", "winners.tsv"));
        OperationStore operationStore = new OperationStore(Path.of("data", "operations.tsv"));
        AdminAuth adminAuth = new AdminAuth();

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/api/health", exchange -> safe(exchange, () -> handleHealth(exchange)));
        server.createContext("/api/config", exchange -> safe(exchange, () -> handlePublicConfig(exchange)));
        server.createContext("/api/admin/login", exchange -> safe(exchange, () -> handleLogin(exchange, adminAuth)));
        server.createContext("/api/admin/logout", exchange -> safe(exchange, () -> handleLogout(exchange)));
        server.createContext("/api/admin/me", exchange -> safe(exchange, () -> requireAdmin(exchange, () -> handleMe(exchange))));
        server.createContext("/api/admin/events", exchange -> safe(exchange, () -> requireAdmin(exchange, () -> handleEvents(exchange, eventStore, submissionStore, winnerStore, operationStore))));
        server.createContext("/api/events", exchange -> safe(exchange, () -> handlePublicEvents(exchange, eventStore, submissionStore, winnerStore)));
        server.createContext("/", exchange -> safe(exchange, () -> serveStatic(exchange, webRoot)));
        ExecutorService requestExecutor = createRequestExecutor();
        server.setExecutor(requestExecutor);
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(0);
            requestExecutor.shutdown();
        }, "jsys-shutdown"));

        System.out.println("J_Sys dev server started");
        System.out.println("App:    http://127.0.0.1:" + port + "/");
        System.out.println("Health: http://127.0.0.1:" + port + "/api/health");
    }

    private static void startChineseInstance(String[] args) throws IOException {
        int port = resolvePort(args);
        Path webRoot = Path.of("frontend", "public").toAbsolutePath().normalize();
        Path dataDirectory = Path.of("data");
        ChineseData data = new ChineseData(dataDirectory.resolve("jsys.db"));
        ChineseData.ChinaAccount chinaAccount = data.bootstrapChinaAccount(
                dataDirectory,
                System.getenv("CHINA_ACCOUNT_EMAIL"),
                System.getenv("CHINA_ACCOUNT_PASSWORD")
        );
        data.bootstrapPlatformAdministrator(
                System.getenv("PLATFORM_ADMIN_EMAIL"),
                System.getenv("PLATFORM_ADMIN_PASSWORD")
        );
        EventStore publicEventStore = new EventStore(data, null);
        SubmissionStore publicSubmissionStore = new SubmissionStore(data, null);
        WinnerStore publicWinnerStore = new WinnerStore(data, null);

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), HTTP_QUEUE_CAPACITY);
        server.createContext("/api/health", exchange -> safe(exchange, () -> handleHealth(exchange)));
        server.createContext("/api/config", exchange -> safe(exchange, () -> handlePublicConfig(exchange)));
        server.createContext("/api/admin/login", exchange -> safe(exchange, () -> handleChineseLogin(exchange, data)));
        server.createContext("/api/admin/register", exchange -> safe(exchange, () -> handleChineseRegistration(exchange, data)));
        server.createContext("/api/admin/logout", exchange -> safe(exchange, () -> handleChineseLogout(exchange, data)));
        server.createContext("/api/admin/me", exchange -> safe(exchange, () -> handleChineseMe(exchange, data)));
        server.createContext("/api/admin/settings", exchange -> safe(exchange, () -> handleChineseSettings(exchange, data)));
        server.createContext("/api/admin/events", exchange -> safe(exchange, () -> handleChineseOwnerEvents(exchange, data)));
        server.createContext("/api/platform/login", exchange -> safe(exchange, () -> handlePlatformLogin(exchange, data)));
        server.createContext("/api/platform/logout", exchange -> safe(exchange, () -> handleChineseLogout(exchange, data)));
        server.createContext("/api/platform/accounts", exchange -> safe(exchange, () -> handlePlatformAccounts(exchange, data)));
        server.createContext("/api/events", exchange -> safe(exchange, () -> handleChinesePublicEvents(exchange, data, publicEventStore, publicSubmissionStore, publicWinnerStore)));
        server.createContext("/platform", exchange -> safe(exchange, () -> servePlatformPage(exchange, webRoot)));
        server.createContext("/join", exchange -> safe(exchange, () -> handleChinesePublicPage(exchange, data, webRoot)));
        server.createContext("/results", exchange -> safe(exchange, () -> handleChinesePublicPage(exchange, data, webRoot)));
        server.createContext("/screen", exchange -> safe(exchange, () -> handleChinesePublicPage(exchange, data, webRoot)));
        server.createContext("/", exchange -> safe(exchange, () -> serveStatic(exchange, webRoot)));
        ExecutorService requestExecutor = createRequestExecutor();
        server.setExecutor(requestExecutor);
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(0);
            requestExecutor.shutdown();
        }, "jsys-chinese-shutdown"));

        System.out.println("J_Sys Chinese instance started");
        System.out.println("App:    http://127.0.0.1:" + port + "/");
        System.out.println("Health: http://127.0.0.1:" + port + "/api/health");
    }

    private static ExecutorService createRequestExecutor() {
        AtomicInteger threadNumber = new AtomicInteger();
        ThreadFactory threadFactory = runnable -> new Thread(
                runnable,
                "jsys-http-" + threadNumber.incrementAndGet()
        );
        return new ThreadPoolExecutor(
                HTTP_WORKER_COUNT,
                HTTP_WORKER_COUNT,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(HTTP_QUEUE_CAPACITY),
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    private static void handleHealth(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
            return;
        }

        String body = "{"
                + "\"status\":\"ok\","
                + "\"service\":\"J_Sys\","
                + "\"time\":\"" + Instant.now() + "\""
                + "}";
        send(exchange, 200, "application/json", body);
    }

    private static void handlePublicConfig(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
            return;
        }
        String locale = "en".equalsIgnoreCase(System.getenv("JSYS_LOCALE")) ? "en" : "bilingual";
        send(exchange, 200, "application/json", "{\"locale\":\"" + locale + "\"}");
    }

    private static void handleLogin(HttpExchange exchange, AdminAuth adminAuth) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
            return;
        }

        Map<String, String> form = readForm(exchange);
        if (!adminAuth.matches(form.get("username"), form.get("password"))) {
            send(exchange, 401, "application/json", "{\"error\":\"Invalid username or password\"}");
            return;
        }

        String token = UUID.randomUUID().toString();
        SESSIONS.put(token, Instant.now());
        exchange.getResponseHeaders().add("Set-Cookie", "jsys_session=" + token + "; Path=/; HttpOnly; SameSite=Lax");
        send(exchange, 200, "application/json", "{\"ok\":true,\"username\":\"" + json(adminAuth.username()) + "\"}");
    }

    private static void handleChineseLogin(HttpExchange exchange, ChineseData data) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
            return;
        }
        Map<String, String> form = readForm(exchange);
        String email = form.containsKey("email") ? form.get("email") : form.get("username");
        Optional<ChineseData.OwnerSession> session = data.loginOwner(email, form.get("password"));
        if (session.isEmpty()) {
            send(exchange, 401, "application/json", "{\"error\":\"Invalid email or password\"}");
            return;
        }
        ChineseData.OwnerSession owner = session.get();
        exchange.getResponseHeaders().add("Set-Cookie", "jsys_session=" + owner.token() + "; Path=/; HttpOnly; SameSite=Lax");
        send(exchange, 200, "application/json", "{\"ok\":true,\"username\":\"" + json(owner.email()) + "\"}");
    }

    private static void handleChineseRegistration(HttpExchange exchange, ChineseData data) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) { send(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}"); return; }
        Map<String, String> form = readForm(exchange);
        try {
            ChineseData.OwnerSession owner = data.registerOwner(form.get("workspaceName"), form.get("email"), form.get("password"));
            exchange.getResponseHeaders().add("Set-Cookie", "jsys_session=" + owner.token() + "; Path=/; HttpOnly; SameSite=Lax");
            send(exchange, 201, "application/json", "{\"ok\":true,\"username\":\"" + json(owner.email()) + "\"}");
        } catch (ChineseData.RegistrationException error) {
            send(exchange, 400, "application/json", "{\"error\":\"" + json(error.getMessage()) + "\"}");
        }
    }

    private static void handleChineseOwnerEvents(HttpExchange exchange, ChineseData data) throws IOException {
        Optional<String> token = readSession(exchange);
        Optional<ChineseData.OwnerSession> owner = token.isEmpty() ? Optional.empty() : data.ownerForSession(token.get());
        if (owner.isEmpty()) { send(exchange, 401, "application/json", "{\"error\":\"Authentication required\"}"); return; }
        ChineseData.OwnerSession session = owner.get();
        handleEvents(exchange, new EventStore(data, session.accountId()), new SubmissionStore(data, session.accountId()),
                new WinnerStore(data, session.accountId()), new OperationStore(data, session.accountId(), session.email()));
    }

    private static void handleChineseSettings(HttpExchange exchange, ChineseData data) throws IOException {
        Optional<String> token = readSession(exchange);
        Optional<ChineseData.OwnerSession> owner = token.isEmpty() ? Optional.empty() : data.ownerForSession(token.get());
        if (owner.isEmpty()) { send(exchange, 401, "application/json", "{\"error\":\"Authentication required\"}"); return; }
        if ("GET".equals(exchange.getRequestMethod())) {
            ChineseData.OwnerSettings settings = data.settings(owner.get());
            send(exchange, 200, "application/json", "{\"workspaceName\":\"" + json(settings.workspaceName()) + "\",\"email\":\"" + json(settings.email()) + "\"}");
            return;
        }
        if ("PUT".equals(exchange.getRequestMethod()) || "POST".equals(exchange.getRequestMethod())) {
            Map<String, String> form = readForm(exchange);
            try {
                ChineseData.OwnerSettings settings = data.updateSettings(owner.get(), form.get("workspaceName"), form.get("currentPassword"), form.get("newPassword"));
                send(exchange, 200, "application/json", "{\"ok\":true,\"workspaceName\":\"" + json(settings.workspaceName()) + "\",\"email\":\"" + json(settings.email()) + "\"}");
            } catch (ChineseData.RegistrationException error) {
                send(exchange, 400, "application/json", "{\"error\":\"" + json(error.getMessage()) + "\"}");
            }
            return;
        }
        send(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
    }

    private static void handlePlatformLogin(HttpExchange exchange, ChineseData data) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) { send(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}"); return; }
        Map<String, String> form = readForm(exchange);
        Optional<ChineseData.PlatformSession> session = data.loginPlatformAdministrator(form.get("email"), form.get("password"));
        if (session.isEmpty()) { send(exchange, 401, "application/json", "{\"error\":\"Invalid email or password\"}"); return; }
        ChineseData.PlatformSession platform = session.get();
        exchange.getResponseHeaders().add("Set-Cookie", "jsys_session=" + platform.token() + "; Path=/; HttpOnly; SameSite=Lax");
        send(exchange, 200, "application/json", "{\"ok\":true,\"email\":\"" + json(platform.email()) + "\"}");
    }

    private static void handlePlatformAccounts(HttpExchange exchange, ChineseData data) throws IOException {
        Optional<String> token = readSession(exchange);
        Optional<ChineseData.PlatformSession> session = token.isEmpty() ? Optional.empty() : data.platformForSession(token.get());
        if (session.isEmpty()) { send(exchange, 401, "application/json", "{\"error\":\"Authentication required\"}"); return; }
        String prefix = "/api/platform/accounts";
        String path = exchange.getRequestURI().getPath();
        String suffix = path.length() > prefix.length() ? path.substring(prefix.length()) : "";
        if (suffix.isEmpty() || "/".equals(suffix)) {
            if (!"GET".equals(exchange.getRequestMethod())) { send(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}"); return; }
            send(exchange, 200, "application/json", branchAccountsJson(data.branchAccounts()));
            return;
        }
        String target = suffix.startsWith("/") ? suffix.substring(1) : suffix;
        int separator = target.lastIndexOf('/');
        if (separator < 1 || !"POST".equals(exchange.getRequestMethod())) { send(exchange, 404, "application/json", "{\"error\":\"Not found\"}"); return; }
        String accountId = target.substring(0, separator);
        String action = target.substring(separator + 1);
        if ("disable".equals(action) || "enable".equals(action)) {
            boolean found = data.setBranchAccountStatus(session.get(), accountId, "enable".equals(action));
            if (!found) { send(exchange, 404, "application/json", "{\"error\":\"Branch Account not found\"}"); return; }
            send(exchange, 200, "application/json", "{\"ok\":true}");
            return;
        }
        if ("password".equals(action)) {
            try {
                boolean found = data.resetBranchOwnerPassword(session.get(), accountId, readForm(exchange).get("newPassword"));
                if (!found) { send(exchange, 404, "application/json", "{\"error\":\"Branch Account not found\"}"); return; }
                send(exchange, 200, "application/json", "{\"ok\":true}");
            } catch (ChineseData.RegistrationException error) {
                send(exchange, 400, "application/json", "{\"error\":\"" + json(error.getMessage()) + "\"}");
            }
            return;
        }
        send(exchange, 404, "application/json", "{\"error\":\"Not found\"}");
    }

    private static void handleChinesePublicEvents(HttpExchange exchange, ChineseData data, EventStore eventStore, SubmissionStore submissionStore, WinnerStore winnerStore) throws IOException {
        String prefix = "/api/events";
        String path = exchange.getRequestURI().getPath();
        String suffix = path.length() > prefix.length() ? path.substring(prefix.length()) : "";
        String target = suffix.startsWith("/") ? suffix.substring(1) : suffix;
        String eventId = target.replaceFirst("/(submissions|results)$", "");
        Optional<String> accountId = eventId.isBlank() || eventId.contains("/") ? Optional.empty() : data.publicEventAccountId(eventId);
        if (accountId.isEmpty()) {
            send(exchange, 404, "application/json", "{\"error\":\"Event not found\"}");
            return;
        }
        if (target.endsWith("/submissions")) {
            if (!"POST".equals(exchange.getRequestMethod())) {
                send(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
                return;
            }
            handleCreateSubmission(exchange, eventStore, new SubmissionStore(data, accountId.get()), eventId);
            return;
        }
        handlePublicEvents(exchange, eventStore, submissionStore, winnerStore);
    }

    private static void handleChinesePublicPage(HttpExchange exchange, ChineseData data, Path webRoot) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod()) && !"HEAD".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "text/plain; charset=utf-8", "Method not allowed");
            return;
        }
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        if (parts.length != 3 || parts[2].isBlank() || !data.isPublicEventAvailable(parts[2])) {
            send(exchange, 404, "text/plain; charset=utf-8", "Not found");
            return;
        }
        serveStatic(exchange, webRoot);
    }

    private static void handleLogout(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
            return;
        }

        readSession(exchange).ifPresent(SESSIONS::remove);
        exchange.getResponseHeaders().add("Set-Cookie", "jsys_session=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
        send(exchange, 200, "application/json", "{\"ok\":true}");
    }

    private static void handleChineseLogout(HttpExchange exchange, ChineseData data) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
            return;
        }
        readSession(exchange).ifPresent(token -> {
            try {
                data.logout(token);
            } catch (IOException error) {
                throw new RuntimeException(error);
            }
        });
        exchange.getResponseHeaders().add("Set-Cookie", "jsys_session=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
        send(exchange, 200, "application/json", "{\"ok\":true}");
    }

    private static void handleMe(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
            return;
        }
        send(exchange, 200, "application/json", "{\"username\":\"" + json(new AdminAuth().username()) + "\"}");
    }

    private static void handleChineseMe(HttpExchange exchange, ChineseData data) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
            return;
        }
        Optional<String> token = readSession(exchange);
        Optional<ChineseData.OwnerSession> owner = token.isEmpty() ? Optional.empty() : data.ownerForSession(token.get());
        if (owner.isEmpty()) {
            send(exchange, 401, "application/json", "{\"error\":\"Authentication required\"}");
            return;
        }
        send(exchange, 200, "application/json", "{\"username\":\"" + json(owner.get().email()) + "\"}");
    }

    private static void handleEvents(
            HttpExchange exchange,
            EventStore eventStore,
            SubmissionStore submissionStore,
            WinnerStore winnerStore,
            OperationStore operationStore
    ) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String prefix = "/api/admin/events";
        String suffix = path.length() > prefix.length() ? path.substring(prefix.length()) : "";

        if (suffix.isEmpty() || "/".equals(suffix)) {
            if ("GET".equals(method)) {
                send(exchange, 200, "application/json", eventsJson(eventStore.list()));
                return;
            }
            if ("POST".equals(method)) {
                EventInput input = EventInput.from(readForm(exchange));
                List<String> errors = input.validate();
                if (!errors.isEmpty()) {
                    send(exchange, 400, "application/json", errorsJson(errors));
                    return;
                }
                Event event = eventStore.create(input);
                send(exchange, 201, "application/json", eventJson(event));
                return;
            }
        }

        String id = suffix.startsWith("/") ? suffix.substring(1) : suffix;
        if (id.endsWith("/draw")) {
            String eventId = id.substring(0, id.length() - "/draw".length());
            if (!"POST".equals(method)) {
                send(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
                return;
            }
            handleDraw(exchange, eventStore, submissionStore, winnerStore, operationStore, eventId);
            return;
        }
        if (id.endsWith("/copy")) {
            String eventId = id.substring(0, id.length() - "/copy".length());
            if (!"POST".equals(method)) {
                send(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
                return;
            }
            Optional<Event> existing = eventStore.find(eventId);
            if (existing.isEmpty()) {
                send(exchange, 404, "application/json", "{\"error\":\"Event not found\"}");
                return;
            }
            Event copied = eventStore.copy(existing.get());
            operationStore.create(copied.id, "copy_event", eventId, "admin");
            send(exchange, 201, "application/json", eventJson(copied));
            return;
        }
        if (id.endsWith("/submissions")) {
            String eventId = id.substring(0, id.length() - "/submissions".length());
            if (!"GET".equals(method)) {
                send(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
                return;
            }
            if (eventStore.find(eventId).isEmpty()) {
                send(exchange, 404, "application/json", "{\"error\":\"Event not found\"}");
                return;
            }
            send(exchange, 200, "application/json", submissionsJson(submissionStore.listByEvent(eventId)));
            return;
        }
        if (id.contains("/submissions/")) {
            String[] parts = id.split("/");
            if (parts.length != 3 || !"submissions".equals(parts[1])) {
                send(exchange, 404, "application/json", "{\"error\":\"Not found\"}");
                return;
            }
            if (!"DELETE".equals(method)) {
                send(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
                return;
            }
            handleDeleteSubmission(exchange, eventStore, submissionStore, winnerStore, operationStore, parts[0], parts[2]);
            return;
        }
        if (id.endsWith("/winners")) {
            String eventId = id.substring(0, id.length() - "/winners".length());
            if (!"GET".equals(method)) {
                send(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
                return;
            }
            if (eventStore.find(eventId).isEmpty()) {
                send(exchange, 404, "application/json", "{\"error\":\"Event not found\"}");
                return;
            }
            send(exchange, 200, "application/json", winnersJson(winnerStore.listByEvent(eventId)));
            return;
        }
        if (id.endsWith("/operations")) {
            String eventId = id.substring(0, id.length() - "/operations".length());
            if (!"GET".equals(method)) {
                send(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
                return;
            }
            if (eventStore.find(eventId).isEmpty()) {
                send(exchange, 404, "application/json", "{\"error\":\"Event not found\"}");
                return;
            }
            send(exchange, 200, "application/json", operationsJson(operationStore.listByEvent(eventId)));
            return;
        }
        if (id.endsWith("/export")) {
            String eventId = id.substring(0, id.length() - "/export".length());
            if (!"GET".equals(method)) {
                send(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
                return;
            }
            handleExport(exchange, eventStore, submissionStore, winnerStore, operationStore, eventId);
            return;
        }
        if (id.contains("/winners/") && id.endsWith("/void")) {
            String[] parts = id.split("/");
            if (parts.length != 4 || !"winners".equals(parts[1]) || !"void".equals(parts[3])) {
                send(exchange, 404, "application/json", "{\"error\":\"Not found\"}");
                return;
            }
            if (!"POST".equals(method)) {
                send(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
                return;
            }
            handleVoidWinner(exchange, eventStore, winnerStore, operationStore, parts[0], parts[2]);
            return;
        }
        if (id.contains("/winners/") && id.endsWith("/redraw")) {
            String[] parts = id.split("/");
            if (parts.length != 4 || !"winners".equals(parts[1]) || !"redraw".equals(parts[3])) {
                send(exchange, 404, "application/json", "{\"error\":\"Not found\"}");
                return;
            }
            if (!"POST".equals(method)) {
                send(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
                return;
            }
            handleRedraw(exchange, eventStore, submissionStore, winnerStore, operationStore, parts[0], parts[2]);
            return;
        }
        if (id.contains("/winners/") && id.endsWith("/replace")) {
            String[] parts = id.split("/");
            if (parts.length != 4 || !"winners".equals(parts[1]) || !"replace".equals(parts[3])) {
                send(exchange, 404, "application/json", "{\"error\":\"Not found\"}");
                return;
            }
            if (!"POST".equals(method)) {
                send(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
                return;
            }
            handleReplaceWinner(exchange, eventStore, submissionStore, winnerStore, operationStore, parts[0], parts[2]);
            return;
        }
        if (id.contains("/winners/")) {
            String[] parts = id.split("/");
            if (parts.length != 3 || !"winners".equals(parts[1])) {
                send(exchange, 404, "application/json", "{\"error\":\"Not found\"}");
                return;
            }
            if (!"DELETE".equals(method)) {
                send(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
                return;
            }
            handleDeleteWinner(exchange, eventStore, winnerStore, operationStore, parts[0], parts[2]);
            return;
        }
        if (id.contains("/")) {
            send(exchange, 404, "application/json", "{\"error\":\"Not found\"}");
            return;
        }

        if ("GET".equals(method)) {
            Optional<Event> event = eventStore.find(id);
            if (event.isEmpty()) {
                send(exchange, 404, "application/json", "{\"error\":\"Event not found\"}");
                return;
            }
            send(exchange, 200, "application/json", eventJson(event.get()));
            return;
        }

        if ("PUT".equals(method)) {
            Optional<Event> existing = eventStore.find(id);
            if (existing.isEmpty()) {
                send(exchange, 404, "application/json", "{\"error\":\"Event not found\"}");
                return;
            }
            EventInput input = EventInput.from(readForm(exchange));
            List<String> errors = input.validate();
            if (!errors.isEmpty()) {
                send(exchange, 400, "application/json", errorsJson(errors));
                return;
            }
            Event updated = eventStore.update(existing.get(), input);
            send(exchange, 200, "application/json", eventJson(updated));
            return;
        }

        if ("DELETE".equals(method)) {
            Optional<Event> existing = eventStore.find(id);
            if (existing.isEmpty()) {
                send(exchange, 404, "application/json", "{\"error\":\"Event not found\"}");
                return;
            }
            eventStore.delete(id);
            submissionStore.deleteByEvent(id);
            winnerStore.deleteByEvent(id);
            operationStore.deleteByEvent(id);
            send(exchange, 200, "application/json", "{\"ok\":true}");
            return;
        }

        send(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
    }

    private static void handlePublicEvents(
            HttpExchange exchange,
            EventStore eventStore,
            SubmissionStore submissionStore,
            WinnerStore winnerStore
    ) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String prefix = "/api/events";
        String suffix = path.length() > prefix.length() ? path.substring(prefix.length()) : "";
        String target = suffix.startsWith("/") ? suffix.substring(1) : suffix;

        if (target.isBlank() || target.contains("//")) {
            send(exchange, 404, "application/json", "{\"error\":\"Not found\"}");
            return;
        }

        if (target.endsWith("/submissions")) {
            String eventId = target.substring(0, target.length() - "/submissions".length());
            if (!"POST".equals(method)) {
                send(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
                return;
            }
            handleCreateSubmission(exchange, eventStore, submissionStore, eventId);
            return;
        }

        if (target.endsWith("/results")) {
            String eventId = target.substring(0, target.length() - "/results".length());
            if (!"GET".equals(method)) {
                send(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
                return;
            }
            if (eventStore.find(eventId).isEmpty()) {
                send(exchange, 404, "application/json", "{\"error\":\"Event not found\"}");
                return;
            }
            List<Winner> validWinners = winnerStore.validByEvent(eventId);
            String state = validWinners.isEmpty() ? "waiting" : "completed";
            send(exchange, 200, "application/json", "{\"state\":\"" + state + "\",\"winners\":" + winnersJson(validWinners) + "}");
            return;
        }

        if ("GET".equals(method)) {
            Optional<Event> event = eventStore.find(target);
            if (event.isEmpty()) {
                send(exchange, 404, "application/json", "{\"error\":\"Event not found\"}");
                return;
            }
            send(exchange, 200, "application/json", eventJson(event.get()));
            return;
        }

        send(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
    }

    private static void handleCreateSubmission(
            HttpExchange exchange,
            EventStore eventStore,
            SubmissionStore submissionStore,
            String eventId
    ) throws IOException {
        Optional<Event> event = eventStore.find(eventId);
        if (event.isEmpty()) {
            send(exchange, 404, "application/json", "{\"error\":\"Event not found\"}");
            return;
        }
        if (!"active".equals(event.get().status)) {
            send(exchange, 409, "application/json", "{\"error\":\"Registration is not available for this event.\"}");
            return;
        }

        SubmissionInput input = SubmissionInput.from(readForm(exchange), event.get().questions);
        List<String> errors = input.validate(event.get().questions);
        if (!errors.isEmpty()) {
            send(exchange, 400, "application/json", errorsJson(errors));
            return;
        }

        Optional<Submission> submission = submissionStore.createIfEmailAbsent(eventId, input);
        if (submission.isEmpty()) {
            send(exchange, 400, "application/json", "{\"errors\":[\"This email has already registered for this event.\"]}");
            return;
        }
        send(exchange, 201, "application/json", submissionJson(submission.get()));
    }

    private static void handleDraw(
            HttpExchange exchange,
            EventStore eventStore,
            SubmissionStore submissionStore,
            WinnerStore winnerStore,
            OperationStore operationStore,
            String eventId
    ) throws IOException {
        Optional<Event> event = eventStore.find(eventId);
        if (event.isEmpty()) {
            send(exchange, 404, "application/json", "{\"error\":\"Event not found\"}");
            return;
        }

        int validWinnerCount = winnerStore.validByEvent(eventId).size();
        if (validWinnerCount >= event.get().winningCount) {
            send(exchange, 409, "application/json", "{\"error\":\"Winner quota is already full.\"}");
            return;
        }

        List<Submission> eligible = eligibleSubmissions(eventId, submissionStore, winnerStore, "");
        if (eligible.isEmpty()) {
            send(exchange, 409, "application/json", "{\"error\":\"No eligible participant left. All registered participants have already won or were used.\"}");
            return;
        }

        Collections.shuffle(eligible);
        Winner winner = winnerStore.create(eventId, eligible.get(0), "draw", "");
        operationStore.create(eventId, "draw", winner.id, "admin");
        send(exchange, 201, "application/json", winnersJson(List.of(winner)));
    }

    private static void handleVoidWinner(
            HttpExchange exchange,
            EventStore eventStore,
            WinnerStore winnerStore,
            OperationStore operationStore,
            String eventId,
            String winnerId
    ) throws IOException {
        if (eventStore.find(eventId).isEmpty()) {
            send(exchange, 404, "application/json", "{\"error\":\"Event not found\"}");
            return;
        }
        Optional<Winner> winner = winnerStore.find(eventId, winnerId);
        if (winner.isEmpty()) {
            send(exchange, 404, "application/json", "{\"error\":\"Winner not found\"}");
            return;
        }
        Winner updated = winnerStore.voidWinner(winner.get());
        operationStore.create(eventId, "void", updated.id, "admin");
        send(exchange, 200, "application/json", winnerJson(updated));
    }

    private static void handleRedraw(
            HttpExchange exchange,
            EventStore eventStore,
            SubmissionStore submissionStore,
            WinnerStore winnerStore,
            OperationStore operationStore,
            String eventId,
            String voidedWinnerId
    ) throws IOException {
        if (eventStore.find(eventId).isEmpty()) {
            send(exchange, 404, "application/json", "{\"error\":\"Event not found\"}");
            return;
        }
        Optional<Winner> voided = winnerStore.find(eventId, voidedWinnerId);
        if (voided.isEmpty()) {
            send(exchange, 404, "application/json", "{\"error\":\"Winner not found\"}");
            return;
        }
        if (!"voided".equals(voided.get().status)) {
            send(exchange, 409, "application/json", "{\"error\":\"Only voided winner records can be redrawn.\"}");
            return;
        }

        List<Submission> eligible = eligibleSubmissions(eventId, submissionStore, winnerStore, voided.get().submissionId);
        if (eligible.isEmpty()) {
            send(exchange, 409, "application/json", "{\"error\":\"No eligible participant for redraw. All registered participants have already won or there are not enough registrations.\"}");
            return;
        }

        Collections.shuffle(eligible);
        Winner replacement = winnerStore.create(eventId, eligible.get(0), "redraw", voidedWinnerId);
        operationStore.create(eventId, "redraw", replacement.id, "admin");
        send(exchange, 201, "application/json", winnerJson(replacement));
    }

    private static void handleReplaceWinner(
            HttpExchange exchange,
            EventStore eventStore,
            SubmissionStore submissionStore,
            WinnerStore winnerStore,
            OperationStore operationStore,
            String eventId,
            String winnerId
    ) throws IOException {
        if (eventStore.find(eventId).isEmpty()) {
            send(exchange, 404, "application/json", "{\"error\":\"Event not found\"}");
            return;
        }
        Optional<Winner> current = winnerStore.find(eventId, winnerId);
        if (current.isEmpty()) {
            send(exchange, 404, "application/json", "{\"error\":\"Winner not found\"}");
            return;
        }
        if (!"valid".equals(current.get().status)) {
            send(exchange, 409, "application/json", "{\"error\":\"Only current valid winners can be replaced.\"}");
            return;
        }

        List<Submission> eligible = eligibleSubmissions(eventId, submissionStore, winnerStore, current.get().submissionId);
        if (eligible.isEmpty()) {
            send(exchange, 409, "application/json", "{\"error\":\"No replacement available. There are not enough remaining eligible participants.\"}");
            return;
        }

        Collections.shuffle(eligible);
        Winner voided = winnerStore.voidWinner(current.get());
        operationStore.create(eventId, "void", voided.id, "admin");
        Winner replacement = winnerStore.create(eventId, eligible.get(0), "redraw", winnerId);
        operationStore.create(eventId, "redraw", replacement.id, "admin");
        send(exchange, 201, "application/json", winnerJson(replacement));
    }

    private static void handleDeleteWinner(
            HttpExchange exchange,
            EventStore eventStore,
            WinnerStore winnerStore,
            OperationStore operationStore,
            String eventId,
            String winnerId
    ) throws IOException {
        if (eventStore.find(eventId).isEmpty()) {
            send(exchange, 404, "application/json", "{\"error\":\"Event not found.\"}");
            return;
        }
        Optional<Winner> winner = winnerStore.find(eventId, winnerId);
        if (winner.isEmpty()) {
            send(exchange, 404, "application/json", "{\"error\":\"Winner not found.\"}");
            return;
        }
        winnerStore.delete(eventId, winnerId);
        operationStore.create(eventId, "delete_winner", winnerId, "admin");
        send(exchange, 200, "application/json", "{\"ok\":true}");
    }

    private static void handleDeleteSubmission(
            HttpExchange exchange,
            EventStore eventStore,
            SubmissionStore submissionStore,
            WinnerStore winnerStore,
            OperationStore operationStore,
            String eventId,
            String submissionId
    ) throws IOException {
        if (eventStore.find(eventId).isEmpty()) {
            send(exchange, 404, "application/json", "{\"error\":\"Event not found\"}");
            return;
        }
        Optional<Submission> submission = submissionStore.find(eventId, submissionId);
        if (submission.isEmpty()) {
            send(exchange, 404, "application/json", "{\"error\":\"Submission not found\"}");
            return;
        }
        submissionStore.delete(eventId, submissionId);
        winnerStore.deleteBySubmission(eventId, submissionId);
        operationStore.create(eventId, "delete_submission", submissionId, "admin");
        send(exchange, 200, "application/json", "{\"ok\":true}");
    }

    private static List<Submission> eligibleSubmissions(
            String eventId,
            SubmissionStore submissionStore,
            WinnerStore winnerStore,
            String excludedSubmissionId
    ) throws IOException {
        List<String> validWinnerSubmissionIds = winnerStore.validSubmissionIds(eventId);
        List<Submission> eligible = new ArrayList<>();
        for (Submission submission : submissionStore.listByEvent(eventId)) {
            if (submission.id.equals(excludedSubmissionId)) {
                continue;
            }
            if (!validWinnerSubmissionIds.contains(submission.id)) {
                eligible.add(submission);
            }
        }
        return eligible;
    }

    private static void handleExport(
            HttpExchange exchange,
            EventStore eventStore,
            SubmissionStore submissionStore,
            WinnerStore winnerStore,
            OperationStore operationStore,
            String eventId
    ) throws IOException {
        Optional<Event> event = eventStore.find(eventId);
        if (event.isEmpty()) {
            send(exchange, 404, "application/json", "{\"error\":\"Event not found\"}");
            return;
        }

        StringBuilder csv = new StringBuilder();
        csv.append('\ufeff');
        List<String> header = new ArrayList<>(List.of(
                "Event ID",
                "Event Title",
                "Name",
                "Job Title",
                "Email"
        ));
        for (Question question : event.get().questions) {
            header.add(question.label);
        }
        header.addAll(List.of(
                "Winner Status",
                "Winning Time",
                "Void Status",
                "Voided Time",
                "Submission Time"
        ));
        csv.append(csvRow(header));

        List<Winner> winners = winnerStore.listByEvent(eventId);
        for (Submission submission : submissionStore.listByEvent(eventId)) {
            Optional<Winner> winner = findLatestWinnerForSubmission(winners, submission.id);
            String winnerStatus = winner.map(item -> item.status).orElse("not_winner");
            String winningTime = winner.map(item -> item.createdAt).orElse("");
            String voidStatus = winner.map(item -> "voided".equals(item.status) ? "voided" : "").orElse("");
            String voidedTime = winner.map(item -> item.voidedAt).orElse("");
            List<String> row = new ArrayList<>(List.of(
                    event.get().id,
                    event.get().title,
                    submission.name,
                    submission.jobTitle,
                    submission.email
            ));
            for (Question question : event.get().questions) {
                row.add(submission.answers.getOrDefault(question.id, ""));
            }
            row.addAll(List.of(
                    winnerStatus,
                    winningTime,
                    voidStatus,
                    voidedTime,
                    submission.createdAt
            ));
            csv.append(csvRow(row));
        }

        Operation operation = operationStore.create(eventId, "export", eventId, "admin");
        exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=utf-8");
        exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"event-" + eventId + "-export.csv\"");
        exchange.getResponseHeaders().set("X-Export-Operation-Id", operation.id);
        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static Optional<Winner> findLatestWinnerForSubmission(List<Winner> winners, String submissionId) {
        Winner latest = null;
        for (Winner winner : winners) {
            if (winner.submissionId.equals(submissionId)) {
                latest = winner;
            }
        }
        return Optional.ofNullable(latest);
    }

    private static String csvRow(List<String> values) {
        StringBuilder row = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                row.append(',');
            }
            row.append(csvCell(values.get(i)));
        }
        return row.append("\r\n").toString();
    }

    private static String csvCell(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private static void requireAdmin(HttpExchange exchange, Handler handler) throws IOException {
        Optional<String> session = readSession(exchange);
        if (session.isEmpty() || !SESSIONS.containsKey(session.get())) {
            send(exchange, 401, "application/json", "{\"error\":\"Authentication required\"}");
            return;
        }
        handler.handle();
    }

    private static void requireChineseOwner(HttpExchange exchange, ChineseData data, Handler handler) throws IOException {
        Optional<String> token = readSession(exchange);
        if (token.isEmpty() || data.ownerForSession(token.get()).isEmpty()) {
            send(exchange, 401, "application/json", "{\"error\":\"Authentication required\"}");
            return;
        }
        handler.handle();
    }

    private static void safe(HttpExchange exchange, Handler handler) throws IOException {
        try {
            handler.handle();
        } catch (Exception error) {
            error.printStackTrace();
            send(exchange, 500, "application/json", "{\"error\":\"Internal server error\"}");
        }
    }

    private static Optional<String> readSession(HttpExchange exchange) {
        List<String> cookies = exchange.getRequestHeaders().get("Cookie");
        if (cookies == null) {
            return Optional.empty();
        }
        for (String cookieHeader : cookies) {
            String[] cookiesInHeader = cookieHeader.split(";");
            for (String cookie : cookiesInHeader) {
                String[] pair = cookie.trim().split("=", 2);
                if (pair.length == 2 && "jsys_session".equals(pair[0]) && !pair[1].isBlank()) {
                    return Optional.of(pair[1]);
                }
            }
        }
        return Optional.empty();
    }

    private static int resolvePort(String[] args) {
        if (args.length > 0 && !args[0].isBlank()) {
            return Integer.parseInt(args[0]);
        }

        String port = System.getenv("PORT");
        if (port != null && !port.isBlank()) {
            return Integer.parseInt(port);
        }

        return DEFAULT_PORT;
    }

    private static void serveStatic(HttpExchange exchange, Path webRoot) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod()) && !"HEAD".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "text/plain; charset=utf-8", "Method not allowed");
            return;
        }

        String requestPath = exchange.getRequestURI().getPath();
        Path target = resolveStaticPath(webRoot, requestPath);
        if (!target.startsWith(webRoot) || !Files.exists(target) || Files.isDirectory(target)) {
            target = webRoot.resolve("index.html");
        }

        byte[] body = Files.readAllBytes(target);
        String contentType = contentType(target);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, "HEAD".equals(exchange.getRequestMethod()) ? -1 : body.length);

        if (!"HEAD".equals(exchange.getRequestMethod())) {
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
        } else {
            exchange.close();
        }
    }

    private static void servePlatformPage(HttpExchange exchange, Path webRoot) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (!("/platform".equals(path) || "/platform/".equals(path))) {
            send(exchange, 404, "text/plain; charset=utf-8", "Not found");
            return;
        }
        if (!"GET".equals(exchange.getRequestMethod()) && !"HEAD".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "text/plain; charset=utf-8", "Method not allowed");
            return;
        }
        Path page = webRoot.resolve("platform.html");
        byte[] body = Files.readAllBytes(page);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, "HEAD".equals(exchange.getRequestMethod()) ? -1 : body.length);
        if (!"HEAD".equals(exchange.getRequestMethod())) {
            try (OutputStream output = exchange.getResponseBody()) { output.write(body); }
        } else {
            exchange.close();
        }
    }

    private static Path resolveStaticPath(Path webRoot, String requestPath) {
        String path = requestPath == null || requestPath.equals("/") ? "/index.html" : requestPath;
        path = path.replace('\\', '/');
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        return webRoot.resolve(path).normalize();
    }

    private static String contentType(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        Map<String, String> types = new HashMap<>();
        types.put(".html", "text/html; charset=utf-8");
        types.put(".css", "text/css; charset=utf-8");
        types.put(".js", "application/javascript; charset=utf-8");
        types.put(".json", "application/json; charset=utf-8");
        types.put(".svg", "image/svg+xml");
        types.put(".png", "image/png");
        types.put(".jpg", "image/jpeg");
        types.put(".jpeg", "image/jpeg");

        for (Map.Entry<String, String> entry : types.entrySet()) {
            if (fileName.endsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "application/octet-stream";
    }

    private static Map<String, String> readForm(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> form = new LinkedHashMap<>();
        if (body.isBlank()) {
            return form;
        }

        for (String pair : body.split("&")) {
            if (pair.isBlank()) {
                continue;
            }
            String[] parts = pair.split("=", 2);
            String key = urlDecode(parts[0]);
            String value = parts.length == 2 ? urlDecode(parts[1]) : "";
            form.put(key, value);
        }
        return form;
    }

    private static void writeLinesReplacing(Path file, List<String> lines) throws IOException {
        Path temp = file.resolveSibling(file.getFileName() + ".tmp-" + UUID.randomUUID());
        Files.write(temp, lines, StandardCharsets.UTF_8);
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private static String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static void send(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", contentType + (contentType.contains("charset") ? "" : "; charset=utf-8"));
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static String eventsJson(List<Event> events) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < events.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(eventJson(events.get(i)));
        }
        return builder.append(']').toString();
    }

    private static String branchAccountsJson(List<ChineseData.BranchAccountSummary> accounts) {
        StringBuilder body = new StringBuilder("[");
        for (int index = 0; index < accounts.size(); index++) {
            if (index > 0) body.append(',');
            ChineseData.BranchAccountSummary account = accounts.get(index);
            body.append("{\"id\":\"").append(json(account.id())).append("\",")
                    .append("\"workspaceName\":\"").append(json(account.workspaceName())).append("\",")
                    .append("\"email\":\"").append(json(account.email())).append("\",")
                    .append("\"status\":\"").append(json(account.status())).append("\"}");
        }
        return body.append(']').toString();
    }

    private static String submissionsJson(List<Submission> submissions) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < submissions.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(submissionJson(submissions.get(i)));
        }
        return builder.append(']').toString();
    }

    private static String winnersJson(List<Winner> winners) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < winners.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(winnerJson(winners.get(i)));
        }
        return builder.append(']').toString();
    }

    private static String winnerJson(Winner winner) {
        return "{"
                + "\"id\":\"" + json(winner.id) + "\","
                + "\"eventId\":\"" + json(winner.eventId) + "\","
                + "\"submissionId\":\"" + json(winner.submissionId) + "\","
                + "\"name\":\"" + json(winner.name) + "\","
                + "\"email\":\"" + json(winner.email) + "\","
                + "\"status\":\"" + json(winner.status) + "\","
                + "\"source\":\"" + json(winner.source) + "\","
                + "\"replacedWinnerId\":\"" + json(winner.replacedWinnerId) + "\","
                + "\"createdAt\":\"" + json(winner.createdAt) + "\","
                + "\"voidedAt\":\"" + json(winner.voidedAt) + "\""
                + "}";
    }

    private static String operationsJson(List<Operation> operations) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < operations.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(operationJson(operations.get(i)));
        }
        return builder.append(']').toString();
    }

    private static String operationJson(Operation operation) {
        return "{"
                + "\"id\":\"" + json(operation.id) + "\","
                + "\"eventId\":\"" + json(operation.eventId) + "\","
                + "\"action\":\"" + json(operation.action) + "\","
                + "\"targetId\":\"" + json(operation.targetId) + "\","
                + "\"operator\":\"" + json(operation.operator) + "\","
                + "\"createdAt\":\"" + json(operation.createdAt) + "\""
                + "}";
    }

    private static String submissionJson(Submission submission) {
        return "{"
                + "\"id\":\"" + json(submission.id) + "\","
                + "\"eventId\":\"" + json(submission.eventId) + "\","
                + "\"name\":\"" + json(submission.name) + "\","
                + "\"jobTitle\":\"" + json(submission.jobTitle) + "\","
                + "\"email\":\"" + json(submission.email) + "\","
                + "\"satisfactionScore\":" + submission.satisfactionScore + ","
                + "\"topicAnswer\":\"" + json(submission.topicAnswer) + "\","
                + "\"futureQuestion\":\"" + json(submission.futureQuestion) + "\","
                + "\"answers\":" + answersJson(submission.answers) + ","
                + "\"createdAt\":\"" + json(submission.createdAt) + "\""
                + "}";
    }

    private static String eventJson(Event event) {
        return "{"
                + "\"id\":\"" + json(event.id) + "\","
                + "\"title\":\"" + json(event.title) + "\","
                + "\"satisfactionQuestion\":\"" + json(event.satisfactionQuestion) + "\","
                + "\"topicQuestion\":\"" + json(event.topicQuestion) + "\","
                + "\"topicOptions\":" + stringsJson(event.topicOptions) + ","
                + "\"freeTextQuestion\":\"" + json(event.freeTextQuestion) + "\","
                + "\"questions\":" + questionsJson(event.questions) + ","
                + "\"privacyNotice\":\"" + json(event.privacyNotice) + "\","
                + "\"winningCount\":" + event.winningCount + ","
                + "\"status\":\"" + json(event.status) + "\","
                + "\"registrationPath\":\"/join/" + json(event.id) + "\","
                + "\"qrTarget\":\"/join/" + json(event.id) + "\","
                + "\"createdAt\":\"" + json(event.createdAt) + "\","
                + "\"updatedAt\":\"" + json(event.updatedAt) + "\""
                + "}";
    }

    private static String questionsJson(List<Question> questions) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < questions.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            Question question = questions.get(i);
            builder.append("{")
                    .append("\"id\":\"").append(json(question.id)).append("\",")
                    .append("\"type\":\"").append(json(question.type)).append("\",")
                    .append("\"label\":\"").append(json(question.label)).append("\",")
                    .append("\"required\":").append(question.required).append(',')
                    .append("\"options\":").append(stringsJson(question.options))
                    .append("}");
        }
        return builder.append(']').toString();
    }

    private static String answersJson(Map<String, String> answers) {
        StringBuilder builder = new StringBuilder("{");
        int index = 0;
        for (Map.Entry<String, String> entry : answers.entrySet()) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append('"').append(json(entry.getKey())).append("\":\"").append(json(entry.getValue())).append('"');
            index += 1;
        }
        return builder.append('}').toString();
    }

    private static String stringsJson(List<String> values) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append('"').append(json(values.get(i))).append('"');
        }
        return builder.append(']').toString();
    }

    private static String errorsJson(List<String> errors) {
        return "{\"errors\":" + stringsJson(errors) + "}";
    }

    private static String json(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    @FunctionalInterface
    private interface Handler {
        void handle() throws IOException;
    }

    private record AdminAuth(String username, String password) {
        AdminAuth() {
            this(
                    envOrDefault("ADMIN_USERNAME", "admin"),
                    envOrDefault("ADMIN_PASSWORD", "admin123")
            );
        }

        boolean matches(String inputUsername, String inputPassword) {
            return username.equals(inputUsername) && password.equals(inputPassword);
        }
    }

    private static String envOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    static final class EventStore {
        private final Path file;
        private final ChineseData chineseData;
        private final String accountId;

        EventStore(Path file) throws IOException {
            this.file = file;
            this.chineseData = null;
            this.accountId = null;
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) {
                Files.createFile(file);
            }
        }

        EventStore(ChineseData chineseData, String accountId) {
            this.file = null;
            this.chineseData = chineseData;
            this.accountId = accountId;
        }

        synchronized List<Event> list() throws IOException {
            if (chineseData != null) {
                return chineseData.listEvents(accountId);
            }
            List<Event> events = new ArrayList<>();
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) {
                    events.add(Event.fromLine(line));
                }
            }
            return events;
        }

        synchronized Optional<Event> find(String id) throws IOException {
            return list().stream().filter(event -> event.id.equals(id)).findFirst();
        }

        synchronized Event create(EventInput input) throws IOException {
            String now = Instant.now().toString();
            Event event = new Event(
                    UUID.randomUUID().toString(),
                    input.title,
                    input.satisfactionQuestion,
                    input.topicQuestion,
                    input.topicOptions,
                    input.freeTextQuestion,
                    input.questions,
                    input.privacyNotice,
                    input.winningCount,
                    input.status,
                    now,
                    now
            );
            List<Event> events = list();
            events.add(event);
            write(events);
            return event;
        }

        synchronized Event copy(Event existing) throws IOException {
            String now = Instant.now().toString();
            Event event = new Event(
                    UUID.randomUUID().toString(),
                    existing.title + " - Copy",
                    existing.satisfactionQuestion,
                    existing.topicQuestion,
                    existing.topicOptions,
                    existing.freeTextQuestion,
                    existing.questions,
                    existing.privacyNotice,
                    existing.winningCount,
                    existing.status,
                    now,
                    now
            );
            List<Event> events = list();
            events.add(event);
            write(events);
            return event;
        }

        synchronized Event update(Event existing, EventInput input) throws IOException {
            Event updated = new Event(
                    existing.id,
                    input.title,
                    input.satisfactionQuestion,
                    input.topicQuestion,
                    input.topicOptions,
                    input.freeTextQuestion,
                    input.questions,
                    input.privacyNotice,
                    input.winningCount,
                    input.status,
                    existing.createdAt,
                    Instant.now().toString()
            );

            List<Event> events = list();
            for (int i = 0; i < events.size(); i++) {
                if (events.get(i).id.equals(existing.id)) {
                    events.set(i, updated);
                    break;
                }
            }
            write(events);
            return updated;
        }

        synchronized void delete(String eventId) throws IOException {
            List<Event> remaining = new ArrayList<>();
            for (Event event : list()) {
                if (!event.id.equals(eventId)) {
                    remaining.add(event);
                }
            }
            write(remaining);
        }

        private void write(List<Event> events) throws IOException {
            if (chineseData != null) {
                chineseData.replaceEvents(accountId, events);
                return;
            }
            List<String> lines = new ArrayList<>();
            for (Event event : events) {
                lines.add(event.toLine());
            }
            writeLinesReplacing(file, lines);
        }
    }

    static final class SubmissionStore {
        private final Path file;
        private final ChineseData chineseData;
        private final String accountId;

        SubmissionStore(Path file) throws IOException {
            this.file = file;
            this.chineseData = null;
            this.accountId = null;
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) {
                Files.createFile(file);
            }
        }

        SubmissionStore(ChineseData chineseData, String accountId) {
            this.file = null;
            this.chineseData = chineseData;
            this.accountId = accountId;
        }

        synchronized List<Submission> list() throws IOException {
            if (chineseData != null) {
                return chineseData.listSubmissions(accountId);
            }
            List<Submission> submissions = new ArrayList<>();
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) {
                    submissions.add(Submission.fromLine(line));
                }
            }
            return submissions;
        }

        synchronized List<Submission> listByEvent(String eventId) throws IOException {
            List<Submission> matches = new ArrayList<>();
            for (Submission submission : list()) {
                if (submission.eventId.equals(eventId)) {
                    matches.add(submission);
                }
            }
            return matches;
        }

        synchronized Optional<Submission> find(String eventId, String submissionId) throws IOException {
            return listByEvent(eventId).stream().filter(submission -> submission.id.equals(submissionId)).findFirst();
        }

        synchronized Optional<Submission> createIfEmailAbsent(String eventId, SubmissionInput input) throws IOException {
            String normalized = normalizeEmail(input.email);
            for (Submission submission : listByEvent(eventId)) {
                if (normalizeEmail(submission.email).equals(normalized)) {
                    return Optional.empty();
                }
            }
            Submission submission = new Submission(
                    UUID.randomUUID().toString(),
                    eventId,
                    input.name,
                    input.jobTitle,
                    input.email,
                    input.satisfactionScore,
                    input.topicAnswer,
                    input.futureQuestion,
                    input.answers,
                    Instant.now().toString()
            );
            List<Submission> submissions = list();
            submissions.add(submission);
            write(submissions);
            return Optional.of(submission);
        }

        synchronized void deleteByEvent(String eventId) throws IOException {
            List<Submission> remaining = new ArrayList<>();
            for (Submission submission : list()) {
                if (!submission.eventId.equals(eventId)) {
                    remaining.add(submission);
                }
            }
            write(remaining);
        }

        synchronized void delete(String eventId, String submissionId) throws IOException {
            List<Submission> remaining = new ArrayList<>();
            for (Submission submission : list()) {
                if (!(submission.eventId.equals(eventId) && submission.id.equals(submissionId))) {
                    remaining.add(submission);
                }
            }
            write(remaining);
        }

        private void write(List<Submission> submissions) throws IOException {
            if (chineseData != null) {
                chineseData.replaceSubmissions(accountId, submissions);
                return;
            }
            List<String> lines = new ArrayList<>();
            for (Submission submission : submissions) {
                lines.add(submission.toLine());
            }
            writeLinesReplacing(file, lines);
        }
    }

    static final class WinnerStore {
        private final Path file;
        private final ChineseData chineseData;
        private final String accountId;

        WinnerStore(Path file) throws IOException {
            this.file = file;
            this.chineseData = null;
            this.accountId = null;
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) {
                Files.createFile(file);
            }
        }

        WinnerStore(ChineseData chineseData, String accountId) {
            this.file = null;
            this.chineseData = chineseData;
            this.accountId = accountId;
        }

        synchronized List<Winner> list() throws IOException {
            if (chineseData != null) {
                return chineseData.listWinners(accountId);
            }
            List<Winner> winners = new ArrayList<>();
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) {
                    winners.add(Winner.fromLine(line));
                }
            }
            return winners;
        }

        synchronized List<Winner> listByEvent(String eventId) throws IOException {
            List<Winner> matches = new ArrayList<>();
            for (Winner winner : list()) {
                if (winner.eventId.equals(eventId)) {
                    matches.add(winner);
                }
            }
            return matches;
        }

        synchronized List<Winner> validByEvent(String eventId) throws IOException {
            List<Winner> matches = new ArrayList<>();
            for (Winner winner : listByEvent(eventId)) {
                if ("valid".equals(winner.status)) {
                    matches.add(winner);
                }
            }
            return matches;
        }

        synchronized List<String> validSubmissionIds(String eventId) throws IOException {
            List<String> ids = new ArrayList<>();
            for (Winner winner : validByEvent(eventId)) {
                ids.add(winner.submissionId);
            }
            return ids;
        }

        synchronized Optional<Winner> find(String eventId, String winnerId) throws IOException {
            return listByEvent(eventId).stream().filter(winner -> winner.id.equals(winnerId)).findFirst();
        }

        synchronized Winner create(String eventId, Submission submission, String source, String replacedWinnerId) throws IOException {
            Winner winner = new Winner(
                    UUID.randomUUID().toString(),
                    eventId,
                    submission.id,
                    submission.name,
                    submission.email,
                    "valid",
                    source,
                    replacedWinnerId,
                    Instant.now().toString(),
                    ""
            );
            List<Winner> winners = list();
            winners.add(winner);
            write(winners);
            return winner;
        }

        synchronized Winner voidWinner(Winner target) throws IOException {
            Winner updated = new Winner(
                    target.id,
                    target.eventId,
                    target.submissionId,
                    target.name,
                    target.email,
                    "voided",
                    target.source,
                    target.replacedWinnerId,
                    target.createdAt,
                    Instant.now().toString()
            );
            List<Winner> winners = list();
            for (int i = 0; i < winners.size(); i++) {
                if (winners.get(i).id.equals(target.id)) {
                    winners.set(i, updated);
                    break;
                }
            }
            write(winners);
            return updated;
        }

        synchronized void delete(String eventId, String winnerId) throws IOException {
            List<Winner> remaining = new ArrayList<>();
            for (Winner winner : list()) {
                if (!(winner.eventId.equals(eventId) && winner.id.equals(winnerId))) {
                    remaining.add(winner);
                }
            }
            write(remaining);
        }

        synchronized void deleteBySubmission(String eventId, String submissionId) throws IOException {
            List<Winner> remaining = new ArrayList<>();
            for (Winner winner : list()) {
                if (!(winner.eventId.equals(eventId) && winner.submissionId.equals(submissionId))) {
                    remaining.add(winner);
                }
            }
            write(remaining);
        }

        synchronized void deleteByEvent(String eventId) throws IOException {
            List<Winner> remaining = new ArrayList<>();
            for (Winner winner : list()) {
                if (!winner.eventId.equals(eventId)) {
                    remaining.add(winner);
                }
            }
            write(remaining);
        }

        private void write(List<Winner> winners) throws IOException {
            if (chineseData != null) {
                chineseData.replaceWinners(accountId, winners);
                return;
            }
            List<String> lines = new ArrayList<>();
            for (Winner winner : winners) {
                lines.add(winner.toLine());
            }
            writeLinesReplacing(file, lines);
        }
    }

    static final class OperationStore {
        private final Path file;
        private final ChineseData chineseData;
        private final String accountId;
        private final String operatorOverride;

        OperationStore(Path file) throws IOException {
            this.file = file;
            this.chineseData = null;
            this.accountId = null;
            this.operatorOverride = null;
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) {
                Files.createFile(file);
            }
        }

        OperationStore(ChineseData chineseData, String accountId) {
            this(chineseData, accountId, null);
        }

        OperationStore(ChineseData chineseData, String accountId, String operatorOverride) {
            this.file = null;
            this.chineseData = chineseData;
            this.accountId = accountId;
            this.operatorOverride = operatorOverride;
        }

        synchronized List<Operation> list() throws IOException {
            if (chineseData != null) {
                return chineseData.listOperations(accountId);
            }
            List<Operation> operations = new ArrayList<>();
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) {
                    operations.add(Operation.fromLine(line));
                }
            }
            return operations;
        }

        synchronized List<Operation> listByEvent(String eventId) throws IOException {
            List<Operation> matches = new ArrayList<>();
            for (Operation operation : list()) {
                if (operation.eventId.equals(eventId)) {
                    matches.add(operation);
                }
            }
            return matches;
        }

        synchronized Operation create(String eventId, String action, String targetId, String operator) throws IOException {
            Operation operation = new Operation(
                    UUID.randomUUID().toString(),
                    eventId,
                    action,
                    targetId,
                    operatorOverride == null ? operator : operatorOverride,
                    Instant.now().toString()
            );
            List<Operation> operations = list();
            operations.add(operation);
            write(operations);
            return operation;
        }

        synchronized void deleteByEvent(String eventId) throws IOException {
            List<Operation> remaining = new ArrayList<>();
            for (Operation operation : list()) {
                if (!operation.eventId.equals(eventId)) {
                    remaining.add(operation);
                }
            }
            write(remaining);
        }

        private void write(List<Operation> operations) throws IOException {
            if (chineseData != null) {
                chineseData.replaceOperations(accountId, operations);
                return;
            }
            List<String> lines = new ArrayList<>();
            for (Operation operation : operations) {
                lines.add(operation.toLine());
            }
            writeLinesReplacing(file, lines);
        }
    }

    private static final class Question {
        static final String TYPE_SINGLE = "single";
        static final String TYPE_MULTIPLE = "multiple";
        static final String TYPE_TEXT = "text";
        static final String TYPE_SCORE = "score";

        final String id;
        final String type;
        final String label;
        final boolean required;
        final List<String> options;

        Question(String id, String type, String label, boolean required, List<String> options) {
            this.id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id.trim();
            this.type = type == null ? "" : type.trim();
            this.label = label == null ? "" : label.trim();
            this.required = required;
            this.options = List.copyOf(options);
        }

        List<String> validate() {
            List<String> errors = new ArrayList<>();
            if (label.isBlank()) {
                errors.add("Question title is required.");
            }
            if (!TYPE_SINGLE.equals(type) && !TYPE_MULTIPLE.equals(type) && !TYPE_TEXT.equals(type) && !TYPE_SCORE.equals(type)) {
                errors.add("Question type must be single, multiple, text, or score.");
            }
            if ((TYPE_SINGLE.equals(type) || TYPE_MULTIPLE.equals(type)) && options.isEmpty()) {
                errors.add("Choice question must have at least one option: " + label);
            }
            return errors;
        }

        static List<Question> defaultQuestions(String satisfactionQuestion, String topicQuestion, List<String> topicOptions, String freeTextQuestion) {
            List<Question> questions = new ArrayList<>();
            questions.add(new Question("score", TYPE_SCORE, satisfactionQuestion, true, List.of()));
            questions.add(new Question("topic", TYPE_SINGLE, topicQuestion, true, topicOptions));
            questions.add(new Question("future", TYPE_TEXT, freeTextQuestion, true, List.of()));
            return questions;
        }

        static List<Question> parseConfig(String value) {
            List<Question> questions = new ArrayList<>();
            if (value == null || value.isBlank()) {
                return questions;
            }
            for (String line : value.split("\\r?\\n")) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split("\\|", -1);
                if (parts.length < 5) {
                    continue;
                }
                List<String> options = new ArrayList<>();
                String decodedOptions = decoded(parts[4]);
                if (!decodedOptions.isBlank()) {
                    for (String option : decodedOptions.split("\\n")) {
                        if (!option.trim().isBlank()) {
                            options.add(option.trim());
                        }
                    }
                }
                questions.add(new Question(
                        decoded(parts[0]),
                        decoded(parts[1]),
                        decoded(parts[2]),
                        Boolean.parseBoolean(decoded(parts[3])),
                        options
                ));
            }
            return questions;
        }

        static String serializeConfig(List<Question> questions) {
            List<String> lines = new ArrayList<>();
            for (Question question : questions) {
                lines.add(String.join("|",
                        encoded(question.id),
                        encoded(question.type),
                        encoded(question.label),
                        encoded(Boolean.toString(question.required)),
                        encoded(String.join("\n", question.options))
                ));
            }
            return String.join("\n", lines);
        }

        static Map<String, String> parseAnswers(String value) {
            Map<String, String> answers = new LinkedHashMap<>();
            if (value == null || value.isBlank()) {
                return answers;
            }
            for (String line : value.split("\\r?\\n")) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split("\\|", 2);
                if (parts.length == 2) {
                    answers.put(decoded(parts[0]), decoded(parts[1]));
                }
            }
            return answers;
        }

        static String serializeAnswers(Map<String, String> answers) {
            List<String> lines = new ArrayList<>();
            for (Map.Entry<String, String> answer : answers.entrySet()) {
                lines.add(encoded(answer.getKey()) + "|" + encoded(answer.getValue()));
            }
            return String.join("\n", lines);
        }

        private static String encoded(String value) {
            return Base64.getUrlEncoder().withoutPadding().encodeToString((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
        }

        private static String decoded(String value) {
            if (value == null || value.isBlank()) {
                return "";
            }
            return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        }
    }

    private static final class EventInput {
        final String title;
        final String satisfactionQuestion;
        final String topicQuestion;
        final List<String> topicOptions;
        final String freeTextQuestion;
        final List<Question> questions;
        final String privacyNotice;
        final int winningCount;
        final String status;

        private EventInput(
                String title,
                String satisfactionQuestion,
                String topicQuestion,
                List<String> topicOptions,
                String freeTextQuestion,
                List<Question> questions,
                String privacyNotice,
                int winningCount,
                String status
        ) {
            this.title = title;
            this.satisfactionQuestion = satisfactionQuestion;
            this.topicQuestion = topicQuestion;
            this.topicOptions = topicOptions;
            this.freeTextQuestion = freeTextQuestion;
            this.questions = List.copyOf(questions);
            this.privacyNotice = privacyNotice;
            this.winningCount = winningCount;
            this.status = status;
        }

        static EventInput from(Map<String, String> form) {
            List<String> options = new ArrayList<>();
            for (String option : form.getOrDefault("topicOptions", "").split("\\r?\\n")) {
                if (!option.trim().isBlank()) {
                    options.add(option.trim());
                }
            }

            int winningCount = -1;
            try {
                winningCount = Integer.parseInt(form.getOrDefault("winningCount", "").trim());
            } catch (NumberFormatException ignored) {
                // Validation reports this below.
            }

            String status = form.getOrDefault("status", "active").trim();
            if (status.isBlank()) {
                status = "active";
            }

            String satisfactionQuestion = form.getOrDefault("satisfactionQuestion", "").trim();
            String topicQuestion = form.getOrDefault("topicQuestion", "").trim();
            String freeTextQuestion = form.getOrDefault("freeTextQuestion", "").trim();
            List<Question> questions = Question.parseConfig(form.getOrDefault("questionsConfig", ""));
            if (questions.isEmpty()) {
                questions = Question.defaultQuestions(satisfactionQuestion, topicQuestion, options, freeTextQuestion);
            }

            return new EventInput(
                    form.getOrDefault("title", "").trim(),
                    satisfactionQuestion,
                    topicQuestion,
                    options,
                    freeTextQuestion,
                    questions,
                    form.getOrDefault("privacyNotice", "").trim(),
                    winningCount,
                    status
            );
        }

        List<String> validate() {
            List<String> errors = new ArrayList<>();
            if (title.isBlank()) {
                errors.add("Event title is required.");
            }
            if (satisfactionQuestion.isBlank()) {
                errors.add("Satisfaction question is required.");
            }
            if (topicQuestion.isBlank()) {
                errors.add("Single-choice question is required.");
            }
            if (topicOptions.isEmpty()) {
                errors.add("At least one single-choice option is required.");
            }
            if (freeTextQuestion.isBlank()) {
                errors.add("Free-text question is required.");
            }
            if (questions.isEmpty()) {
                errors.add("At least one registration question is required.");
            }
            for (Question question : questions) {
                errors.addAll(question.validate());
            }
            if (privacyNotice.isBlank()) {
                errors.add("Privacy notice is required.");
            }
            if (winningCount <= 0) {
                errors.add("Winning count must be a positive number.");
            }
            if (!status.equals("active") && !status.equals("draft") && !status.equals("closed")) {
                errors.add("Status must be active, draft, or closed.");
            }
            return errors;
        }
    }

    private static final class SubmissionInput {
        final String name;
        final String jobTitle;
        final String email;
        final int satisfactionScore;
        final String topicAnswer;
        final String futureQuestion;
        final Map<String, String> answers;

        private SubmissionInput(
                String name,
                String jobTitle,
                String email,
                int satisfactionScore,
                String topicAnswer,
                String futureQuestion,
                Map<String, String> answers
        ) {
            this.name = name;
            this.jobTitle = jobTitle;
            this.email = email;
            this.satisfactionScore = satisfactionScore;
            this.topicAnswer = topicAnswer;
            this.futureQuestion = futureQuestion;
            this.answers = Map.copyOf(answers);
        }

        static SubmissionInput from(Map<String, String> form, List<Question> questions) {
            int score = -1;
            try {
                score = Integer.parseInt(form.getOrDefault("satisfactionScore", "").trim());
            } catch (NumberFormatException ignored) {
                // Validation reports this below.
            }
            Map<String, String> answers = new LinkedHashMap<>();
            for (Question question : questions) {
                answers.put(question.id, form.getOrDefault("answer_" + question.id, "").trim());
            }
            if (answers.isEmpty()) {
                answers.put("score", form.getOrDefault("satisfactionScore", "").trim());
                answers.put("topic", form.getOrDefault("topicAnswer", "").trim());
                answers.put("future", form.getOrDefault("futureQuestion", "").trim());
            }

            return new SubmissionInput(
                    form.getOrDefault("name", "").trim(),
                    form.getOrDefault("jobTitle", "").trim(),
                    form.getOrDefault("email", "").trim(),
                    score,
                    form.getOrDefault("topicAnswer", "").trim(),
                    form.getOrDefault("futureQuestion", "").trim(),
                    answers
            );
        }

        List<String> validate(List<Question> questions) {
            List<String> errors = new ArrayList<>();
            if (name.isBlank()) {
                errors.add("Name is required.");
            }
            if (jobTitle.isBlank()) {
                errors.add("Job title is required.");
            }
            if (email.isBlank()) {
                errors.add("Email is required.");
            } else if (!isValidEmail(email)) {
                errors.add("Email format is invalid.");
            }
            for (Question question : questions) {
                String answer = answers.getOrDefault(question.id, "");
                if (question.required && answer.isBlank()) {
                    errors.add("Question is required: " + question.label);
                }
                if ("score".equals(question.type) && !answer.isBlank()) {
                    try {
                        int answerScore = Integer.parseInt(answer);
                        if (answerScore < 1 || answerScore > 10) {
                            errors.add("Score answer must be between 1 and 10: " + question.label);
                        }
                    } catch (NumberFormatException error) {
                        errors.add("Score answer must be a number: " + question.label);
                    }
                }
                if (("single".equals(question.type) || "multiple".equals(question.type)) && !answer.isBlank()) {
                    for (String item : answer.split("\\n")) {
                        if (!question.options.contains(item)) {
                            errors.add("Invalid option for question: " + question.label);
                            break;
                        }
                    }
                }
            }
            return errors;
        }

        private static boolean isValidEmail(String email) {
            int at = email.indexOf('@');
            int dot = email.lastIndexOf('.');
            return at > 0 && dot > at + 1 && dot < email.length() - 1 && !email.contains(" ");
        }
    }

    static final class Event {
        final String id;
        final String title;
        final String satisfactionQuestion;
        final String topicQuestion;
        final List<String> topicOptions;
        final String freeTextQuestion;
        final List<Question> questions;
        final String privacyNotice;
        final int winningCount;
        final String status;
        final String createdAt;
        final String updatedAt;

        Event(
                String id,
                String title,
                String satisfactionQuestion,
                String topicQuestion,
                List<String> topicOptions,
                String freeTextQuestion,
                List<Question> questions,
                String privacyNotice,
                int winningCount,
                String status,
                String createdAt,
                String updatedAt
        ) {
            this.id = id;
            this.title = title;
            this.satisfactionQuestion = satisfactionQuestion;
            this.topicQuestion = topicQuestion;
            this.topicOptions = List.copyOf(topicOptions);
            this.freeTextQuestion = freeTextQuestion;
            this.questions = List.copyOf(questions);
            this.privacyNotice = privacyNotice;
            this.winningCount = winningCount;
            this.status = status;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        String toLine() {
            return String.join("\t",
                    encoded(id),
                    encoded(title),
                    encoded(satisfactionQuestion),
                    encoded(topicQuestion),
                    encoded(String.join("\n", topicOptions)),
                    encoded(freeTextQuestion),
                    encoded(Question.serializeConfig(questions)),
                    encoded(privacyNotice),
                    Integer.toString(winningCount),
                    encoded(status),
                    encoded(createdAt),
                    encoded(updatedAt)
            );
        }

        static Event fromLine(String line) {
            String[] parts = line.split("\t", -1);
            List<String> options = new ArrayList<>();
            if (parts.length > 4) {
                for (String option : decoded(parts[4]).split("\\n")) {
                    if (!option.isBlank()) {
                        options.add(option);
                    }
                }
            }
            String satisfactionQuestion = decoded(part(parts, 2));
            String topicQuestion = decoded(part(parts, 3));
            String freeTextQuestion;
            List<Question> questions;
            String privacyNotice;
            int winningCount;
            String status;
            String createdAt;
            String updatedAt;
            if (parts.length > 11) {
                freeTextQuestion = decoded(part(parts, 5));
                questions = Question.parseConfig(decoded(part(parts, 6)));
                if (questions.isEmpty()) {
                    questions = Question.defaultQuestions(satisfactionQuestion, topicQuestion, options, freeTextQuestion);
                }
                privacyNotice = decoded(part(parts, 7));
                winningCount = Integer.parseInt(part(parts, 8, "1"));
                status = decoded(part(parts, 9, "active"));
                createdAt = decoded(part(parts, 10));
                updatedAt = decoded(part(parts, 11));
            } else {
                freeTextQuestion = decoded(part(parts, 5));
                questions = Question.defaultQuestions(satisfactionQuestion, topicQuestion, options, freeTextQuestion);
                privacyNotice = decoded(part(parts, 6));
                winningCount = Integer.parseInt(part(parts, 7, "1"));
                status = decoded(part(parts, 8, "active"));
                createdAt = decoded(part(parts, 9));
                updatedAt = decoded(part(parts, 10));
            }

            return new Event(
                    decoded(part(parts, 0)),
                    decoded(part(parts, 1)),
                    satisfactionQuestion,
                    topicQuestion,
                    options,
                    freeTextQuestion,
                    questions,
                    privacyNotice,
                    winningCount,
                    status,
                    createdAt,
                    updatedAt
            );
        }

        private static String part(String[] parts, int index) {
            return part(parts, index, "");
        }

        private static String part(String[] parts, int index, String defaultValue) {
            return index < parts.length ? parts[index] : defaultValue;
        }

        private static String encoded(String value) {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
        }

        private static String decoded(String value) {
            if (value == null || value.isBlank()) {
                return "";
            }
            return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        }
    }

    static final class Submission {
        final String id;
        final String eventId;
        final String name;
        final String jobTitle;
        final String email;
        final int satisfactionScore;
        final String topicAnswer;
        final String futureQuestion;
        final Map<String, String> answers;
        final String createdAt;

        Submission(
                String id,
                String eventId,
                String name,
                String jobTitle,
                String email,
                int satisfactionScore,
                String topicAnswer,
                String futureQuestion,
                Map<String, String> answers,
                String createdAt
        ) {
            this.id = id;
            this.eventId = eventId;
            this.name = name;
            this.jobTitle = jobTitle;
            this.email = email;
            this.satisfactionScore = satisfactionScore;
            this.topicAnswer = topicAnswer;
            this.futureQuestion = futureQuestion;
            this.answers = Map.copyOf(answers);
            this.createdAt = createdAt;
        }

        String toLine() {
            return String.join("\t",
                    encoded(id),
                    encoded(eventId),
                    encoded(name),
                    encoded(jobTitle),
                    encoded(email),
                    Integer.toString(satisfactionScore),
                    encoded(topicAnswer),
                    encoded(futureQuestion),
                    encoded(Question.serializeAnswers(answers)),
                    encoded(createdAt)
            );
        }

        static Submission fromLine(String line) {
            String[] parts = line.split("\t", -1);
            Map<String, String> answers;
            String createdAt;
            if (parts.length > 9) {
                answers = Question.parseAnswers(decoded(part(parts, 8)));
                createdAt = decoded(part(parts, 9));
            } else {
                answers = new LinkedHashMap<>();
                answers.put("score", part(parts, 5, "0"));
                answers.put("topic", decoded(part(parts, 6)));
                answers.put("future", decoded(part(parts, 7)));
                createdAt = decoded(part(parts, 8));
            }
            return new Submission(
                    decoded(part(parts, 0)),
                    decoded(part(parts, 1)),
                    decoded(part(parts, 2)),
                    decoded(part(parts, 3)),
                    decoded(part(parts, 4)),
                    Integer.parseInt(part(parts, 5, "0")),
                    decoded(part(parts, 6)),
                    decoded(part(parts, 7)),
                    answers,
                    createdAt
            );
        }

        private static String part(String[] parts, int index) {
            return part(parts, index, "");
        }

        private static String part(String[] parts, int index, String defaultValue) {
            return index < parts.length ? parts[index] : defaultValue;
        }

        private static String encoded(String value) {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
        }

        private static String decoded(String value) {
            if (value == null || value.isBlank()) {
                return "";
            }
            return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        }
    }

    static final class Winner {
        final String id;
        final String eventId;
        final String submissionId;
        final String name;
        final String email;
        final String status;
        final String source;
        final String replacedWinnerId;
        final String createdAt;
        final String voidedAt;

        Winner(
                String id,
                String eventId,
                String submissionId,
                String name,
                String email,
                String status,
                String source,
                String replacedWinnerId,
                String createdAt,
                String voidedAt
        ) {
            this.id = id;
            this.eventId = eventId;
            this.submissionId = submissionId;
            this.name = name;
            this.email = email;
            this.status = status;
            this.source = source;
            this.replacedWinnerId = replacedWinnerId;
            this.createdAt = createdAt;
            this.voidedAt = voidedAt;
        }

        String toLine() {
            return String.join("\t",
                    encoded(id),
                    encoded(eventId),
                    encoded(submissionId),
                    encoded(name),
                    encoded(email),
                    encoded(status),
                    encoded(source),
                    encoded(replacedWinnerId),
                    encoded(createdAt),
                    encoded(voidedAt)
            );
        }

        static Winner fromLine(String line) {
            String[] parts = line.split("\t", -1);
            return new Winner(
                    decoded(part(parts, 0)),
                    decoded(part(parts, 1)),
                    decoded(part(parts, 2)),
                    decoded(part(parts, 3)),
                    decoded(part(parts, 4)),
                    decoded(part(parts, 5, "valid")),
                    decoded(part(parts, 6)),
                    decoded(part(parts, 7)),
                    decoded(part(parts, 8)),
                    decoded(part(parts, 9))
            );
        }

        private static String part(String[] parts, int index) {
            return part(parts, index, "");
        }

        private static String part(String[] parts, int index, String defaultValue) {
            return index < parts.length ? parts[index] : defaultValue;
        }

        private static String encoded(String value) {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
        }

        private static String decoded(String value) {
            if (value == null || value.isBlank()) {
                return "";
            }
            return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        }
    }

    static final class Operation {
        final String id;
        final String eventId;
        final String action;
        final String targetId;
        final String operator;
        final String createdAt;

        Operation(String id, String eventId, String action, String targetId, String operator, String createdAt) {
            this.id = id;
            this.eventId = eventId;
            this.action = action;
            this.targetId = targetId;
            this.operator = operator;
            this.createdAt = createdAt;
        }

        String toLine() {
            return String.join("\t",
                    encoded(id),
                    encoded(eventId),
                    encoded(action),
                    encoded(targetId),
                    encoded(operator),
                    encoded(createdAt)
            );
        }

        static Operation fromLine(String line) {
            String[] parts = line.split("\t", -1);
            return new Operation(
                    decoded(part(parts, 0)),
                    decoded(part(parts, 1)),
                    decoded(part(parts, 2)),
                    decoded(part(parts, 3)),
                    decoded(part(parts, 4)),
                    decoded(part(parts, 5))
            );
        }

        private static String part(String[] parts, int index) {
            return index < parts.length ? parts[index] : "";
        }

        private static String encoded(String value) {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
        }

        private static String decoded(String value) {
            if (value == null || value.isBlank()) {
                return "";
            }
            return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        }
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
