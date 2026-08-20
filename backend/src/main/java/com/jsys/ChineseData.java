package com.jsys;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

final class ChineseData {
    static final String CHINA_WORKSPACE_NAME = "中国账号";
    private static final String MIGRATION_KEY = "legacy-tsv-import-v1";
    private static final String PLATFORM_ADMIN_KEY = "platform-admin-seed-v1";
    private static final int PASSWORD_ITERATIONS = 210_000;
    private static final int PASSWORD_KEY_LENGTH = 256;

    private final Path databaseFile;

    ChineseData(Path databaseFile) throws IOException {
        this.databaseFile = databaseFile.toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.databaseFile.getParent());
            Class.forName("org.sqlite.JDBC");
            try (Connection connection = open()) {
                createSchema(connection);
            }
        } catch (ClassNotFoundException | SQLException error) {
            throw new IOException("Unable to initialize the Chinese-instance SQLite database", error);
        }
    }

    ChinaAccount bootstrapChinaAccount(Path legacyDataDirectory, String email, String password) throws IOException {
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            try {
                Optional<String> imported = metadata(connection, MIGRATION_KEY);
                if (imported.isPresent()) {
                    ChinaAccount account = findChinaAccount(connection);
                    connection.commit();
                    return account;
                }
                if (!validEmail(email) || password == null || password.length() < 8 || password.length() > 128) {
                    throw new IOException("CHINA_ACCOUNT_EMAIL and CHINA_ACCOUNT_PASSWORD are required for the first Chinese-instance migration");
                }

                String accountId = UUID.randomUUID().toString();
                String now = Instant.now().toString();
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO branch_accounts(id, workspace_name, status, created_at) VALUES (?, ?, 'active', ?)")) {
                    statement.setString(1, accountId);
                    statement.setString(2, CHINA_WORKSPACE_NAME);
                    statement.setString(3, now);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO identities(id, account_id, role, email, password_hash, created_at) VALUES (?, ?, 'owner', ?, ?, ?)")) {
                    statement.setString(1, UUID.randomUUID().toString());
                    statement.setString(2, accountId);
                    statement.setString(3, email);
                    statement.setString(4, passwordHash(password));
                    statement.setString(5, now);
                    statement.executeUpdate();
                }

                replaceEvents(connection, accountId, readEvents(legacyDataDirectory.resolve("events.tsv")));
                replaceSubmissions(connection, accountId, readSubmissions(legacyDataDirectory.resolve("submissions.tsv")));
                replaceWinners(connection, accountId, readWinners(legacyDataDirectory.resolve("winners.tsv")));
                replaceOperations(connection, accountId, readOperations(legacyDataDirectory.resolve("operations.tsv")));
                putMetadata(connection, MIGRATION_KEY, now);
                audit(connection, accountId, email, "migrate_legacy_data", accountId, "success");
                connection.commit();
                return new ChinaAccount(accountId, email);
            } catch (Exception error) {
                connection.rollback();
                if (error instanceof IOException ioError) {
                    throw ioError;
                }
                if (error instanceof SQLException sqlError) {
                    throw new IOException("Unable to migrate legacy TSV data", sqlError);
                }
                throw new IOException("Unable to migrate legacy TSV data", error);
            }
        } catch (SQLException error) {
            throw new IOException("Unable to open the Chinese-instance SQLite database", error);
        }
    }

    void bootstrapPlatformAdministrator(String email, String password) throws IOException {
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            try {
                if (metadata(connection, PLATFORM_ADMIN_KEY).isPresent()) {
                    connection.commit();
                    return;
                }
                if (!validEmail(email) || password == null || password.length() < 8 || password.length() > 128) {
                    throw new IOException("PLATFORM_ADMIN_EMAIL and PLATFORM_ADMIN_PASSWORD are required for the first Chinese-instance start");
                }
                String identityId = UUID.randomUUID().toString();
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO identities(id, account_id, role, email, password_hash, created_at) VALUES (?, NULL, 'platform_admin', ?, ?, ?)")) {
                    statement.setString(1, identityId);
                    statement.setString(2, email);
                    statement.setString(3, passwordHash(password));
                    statement.setString(4, Instant.now().toString());
                    statement.executeUpdate();
                }
                putMetadata(connection, PLATFORM_ADMIN_KEY, Instant.now().toString());
                platformAudit(connection, email, "seed_platform_administrator", identityId, "success");
                connection.commit();
            } catch (Exception error) {
                connection.rollback();
                if (error instanceof IOException ioError) throw ioError;
                if (error instanceof SQLException sqlError) throw new IOException("Unable to seed Platform Administrator", sqlError);
                throw new IOException("Unable to seed Platform Administrator", error);
            }
        } catch (SQLException error) {
            throw new IOException("Unable to open the Chinese-instance SQLite database", error);
        }
    }

    synchronized Optional<OwnerSession> loginOwner(String email, String password) throws IOException {
        if (email == null || password == null) {
            return Optional.empty();
        }
        String query = "SELECT i.id, i.account_id, i.email, i.password_hash, a.status "
                + "FROM identities i JOIN branch_accounts a ON a.id = i.account_id "
                + "WHERE i.role = 'owner' AND i.email = ?";
        try (Connection connection = open(); PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, email);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || !"active".equals(result.getString("status"))
                        || !passwordMatches(password, result.getString("password_hash"))) {
                    return Optional.empty();
                }
                String token = UUID.randomUUID().toString();
                String expiresAt = Instant.now().plus(7, ChronoUnit.DAYS).toString();
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO sessions(token, identity_id, expires_at, revoked_at) VALUES (?, ?, ?, NULL)")) {
                    insert.setString(1, token);
                    insert.setString(2, result.getString("id"));
                    insert.setString(3, expiresAt);
                    insert.executeUpdate();
                }
                audit(connection, result.getString("account_id"), result.getString("email"), "login", result.getString("id"), "success");
                return Optional.of(new OwnerSession(token, result.getString("id"), result.getString("account_id"), result.getString("email")));
            }
        } catch (SQLException error) {
            throw new IOException("Unable to sign in China Account Owner", error);
        }
    }

    synchronized Optional<OwnerSession> ownerForSession(String token) throws IOException {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String query = "SELECT s.token, i.id AS identity_id, i.account_id, i.email, a.status, s.expires_at, s.revoked_at "
                + "FROM sessions s JOIN identities i ON i.id = s.identity_id "
                + "JOIN branch_accounts a ON a.id = i.account_id "
                + "WHERE s.token = ? AND i.role = 'owner'";
        try (Connection connection = open(); PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, token);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || result.getString("revoked_at") != null || !"active".equals(result.getString("status"))
                        || !Instant.parse(result.getString("expires_at")).isAfter(Instant.now())) {
                    return Optional.empty();
                }
                return Optional.of(new OwnerSession(result.getString("token"), result.getString("identity_id"), result.getString("account_id"), result.getString("email")));
            }
        } catch (SQLException error) {
            throw new IOException("Unable to validate China Account Owner session", error);
        }
    }

    synchronized Optional<PlatformSession> loginPlatformAdministrator(String email, String password) throws IOException {
        if (email == null || password == null) return Optional.empty();
        try (Connection connection = open(); PreparedStatement statement = connection.prepareStatement(
                "SELECT id, email, password_hash FROM identities WHERE role = 'platform_admin' AND email = ?")) {
            statement.setString(1, email);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || !passwordMatches(password, result.getString("password_hash"))) return Optional.empty();
                String token = UUID.randomUUID().toString();
                try (PreparedStatement session = connection.prepareStatement(
                        "INSERT INTO sessions(token, identity_id, expires_at, revoked_at) VALUES (?, ?, ?, NULL)")) {
                    session.setString(1, token);
                    session.setString(2, result.getString("id"));
                    session.setString(3, Instant.now().plus(7, ChronoUnit.DAYS).toString());
                    session.executeUpdate();
                }
                platformAudit(connection, result.getString("email"), "platform_login", result.getString("id"), "success");
                return Optional.of(new PlatformSession(token, result.getString("id"), result.getString("email")));
            }
        } catch (SQLException error) {
            throw new IOException("Unable to sign in Platform Administrator", error);
        }
    }

    synchronized Optional<PlatformSession> platformForSession(String token) throws IOException {
        if (token == null || token.isBlank()) return Optional.empty();
        String query = "SELECT s.token, i.id AS identity_id, i.email, s.expires_at, s.revoked_at "
                + "FROM sessions s JOIN identities i ON i.id = s.identity_id "
                + "WHERE s.token = ? AND i.role = 'platform_admin'";
        try (Connection connection = open(); PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, token);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || result.getString("revoked_at") != null
                        || !Instant.parse(result.getString("expires_at")).isAfter(Instant.now())) return Optional.empty();
                return Optional.of(new PlatformSession(result.getString("token"), result.getString("identity_id"), result.getString("email")));
            }
        } catch (SQLException error) {
            throw new IOException("Unable to validate Platform Administrator session", error);
        }
    }

    synchronized List<BranchAccountSummary> branchAccounts() throws IOException {
        String query = "SELECT a.id, a.workspace_name, a.status, i.email FROM branch_accounts a "
                + "JOIN identities i ON i.account_id = a.id AND i.role = 'owner' ORDER BY a.created_at";
        try (Connection connection = open(); PreparedStatement statement = connection.prepareStatement(query); ResultSet result = statement.executeQuery()) {
            List<BranchAccountSummary> accounts = new ArrayList<>();
            while (result.next()) accounts.add(new BranchAccountSummary(result.getString("id"), result.getString("workspace_name"), result.getString("email"), result.getString("status")));
            return accounts;
        } catch (SQLException error) { throw new IOException("Unable to load Branch Accounts", error); }
    }

    synchronized boolean setBranchAccountStatus(PlatformSession platform, String accountId, boolean active) throws IOException {
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            try (PreparedStatement update = connection.prepareStatement("UPDATE branch_accounts SET status = ? WHERE id = ?")) {
                update.setString(1, active ? "active" : "disabled");
                update.setString(2, accountId);
                if (update.executeUpdate() != 1) { connection.rollback(); return false; }
            }
            if (!active) {
                try (PreparedStatement revoke = connection.prepareStatement(
                        "UPDATE sessions SET revoked_at = ? WHERE identity_id IN (SELECT id FROM identities WHERE account_id = ?) AND revoked_at IS NULL")) {
                    revoke.setString(1, Instant.now().toString()); revoke.setString(2, accountId); revoke.executeUpdate();
                }
            }
            platformAudit(connection, platform.email(), active ? "enable_account" : "disable_account", accountId, "success");
            connection.commit();
            return true;
        } catch (SQLException error) { throw new IOException("Unable to update Branch Account status", error); }
    }

    synchronized boolean resetBranchOwnerPassword(PlatformSession platform, String accountId, String newPassword) throws RegistrationException, IOException {
        if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 128) throw new RegistrationException("Password must contain 8 to 128 characters");
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            try (PreparedStatement password = connection.prepareStatement("UPDATE identities SET password_hash = ? WHERE account_id = ? AND role = 'owner'")) {
                password.setString(1, passwordHash(newPassword)); password.setString(2, accountId);
                if (password.executeUpdate() != 1) { connection.rollback(); return false; }
            }
            try (PreparedStatement revoke = connection.prepareStatement(
                    "UPDATE sessions SET revoked_at = ? WHERE identity_id IN (SELECT id FROM identities WHERE account_id = ?) AND revoked_at IS NULL")) {
                revoke.setString(1, Instant.now().toString()); revoke.setString(2, accountId); revoke.executeUpdate();
            }
            platformAudit(connection, platform.email(), "reset_owner_password", accountId, "success");
            connection.commit();
            return true;
        } catch (SQLException error) { throw new IOException("Unable to reset Branch Account Owner password", error); }
    }

    synchronized void logout(String token) throws IOException {
        if (token == null || token.isBlank()) {
            return;
        }
        try (Connection connection = open(); PreparedStatement statement = connection.prepareStatement(
                "UPDATE sessions SET revoked_at = ? WHERE token = ? AND revoked_at IS NULL")) {
            statement.setString(1, Instant.now().toString());
            statement.setString(2, token);
            statement.executeUpdate();
        } catch (SQLException error) {
            throw new IOException("Unable to sign out China Account Owner", error);
        }
    }

    synchronized OwnerSettings settings(OwnerSession owner) throws IOException {
        try (Connection connection = open(); PreparedStatement statement = connection.prepareStatement(
                "SELECT workspace_name FROM branch_accounts WHERE id = ?")) {
            statement.setString(1, owner.accountId());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new IOException("Branch Account not found");
                return new OwnerSettings(result.getString("workspace_name"), owner.email());
            }
        } catch (SQLException error) { throw new IOException("Unable to load Owner settings", error); }
    }

    synchronized OwnerSettings updateSettings(OwnerSession owner, String workspaceName, String currentPassword, String newPassword) throws RegistrationException, IOException {
        String name = workspaceName == null ? "" : workspaceName.trim();
        if (name.isEmpty() || name.length() > 100) throw new RegistrationException("Workspace name must contain 1 to 100 characters");
        if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 128) throw new RegistrationException("Password must contain 8 to 128 characters");
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            try (PreparedStatement identity = connection.prepareStatement("SELECT password_hash FROM identities WHERE id = ?")) {
                identity.setString(1, owner.identityId());
                try (ResultSet result = identity.executeQuery()) {
                    if (!result.next() || !passwordMatches(currentPassword == null ? "" : currentPassword, result.getString("password_hash"))) {
                        throw new RegistrationException("Current password is incorrect");
                    }
                }
            }
            try (PreparedStatement account = connection.prepareStatement("UPDATE branch_accounts SET workspace_name = ? WHERE id = ?");
                 PreparedStatement identity = connection.prepareStatement("UPDATE identities SET password_hash = ? WHERE id = ?");
                 PreparedStatement sessions = connection.prepareStatement("UPDATE sessions SET revoked_at = ? WHERE identity_id = ? AND revoked_at IS NULL")) {
                account.setString(1, name); account.setString(2, owner.accountId()); account.executeUpdate();
                identity.setString(1, passwordHash(newPassword)); identity.setString(2, owner.identityId()); identity.executeUpdate();
                sessions.setString(1, Instant.now().toString()); sessions.setString(2, owner.identityId()); sessions.executeUpdate();
            }
            audit(connection, owner.accountId(), owner.email(), "update_settings", owner.accountId(), "success");
            connection.commit();
            return new OwnerSettings(name, owner.email());
        } catch (SQLException error) { throw new IOException("Unable to update Owner settings", error); }
    }

    synchronized OwnerSession registerOwner(String workspaceName, String email, String password) throws RegistrationException, IOException {
        String name = workspaceName == null ? "" : workspaceName.trim();
        if (name.isEmpty() || name.length() > 100) throw new RegistrationException("Workspace name must contain 1 to 100 characters");
        if (!validEmail(email)) throw new RegistrationException("A valid email address is required");
        if (password == null || password.length() < 8 || password.length() > 128) throw new RegistrationException("Password must contain 8 to 128 characters");
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            try {
                String accountId = UUID.randomUUID().toString();
                String identityId = UUID.randomUUID().toString();
                String now = Instant.now().toString();
                try (PreparedStatement account = connection.prepareStatement("INSERT INTO branch_accounts(id, workspace_name, status, created_at) VALUES (?, ?, 'active', ?)");
                     PreparedStatement identity = connection.prepareStatement("INSERT INTO identities(id, account_id, role, email, password_hash, created_at) VALUES (?, ?, 'owner', ?, ?, ?)")) {
                    account.setString(1, accountId); account.setString(2, name); account.setString(3, now); account.executeUpdate();
                    identity.setString(1, identityId); identity.setString(2, accountId); identity.setString(3, email); identity.setString(4, passwordHash(password)); identity.setString(5, now); identity.executeUpdate();
                }
                String token = UUID.randomUUID().toString();
                try (PreparedStatement session = connection.prepareStatement("INSERT INTO sessions(token, identity_id, expires_at, revoked_at) VALUES (?, ?, ?, NULL)")) {
                    session.setString(1, token); session.setString(2, identityId); session.setString(3, Instant.now().plus(7, ChronoUnit.DAYS).toString()); session.executeUpdate();
                }
                audit(connection, accountId, email, "register", accountId, "success");
                connection.commit();
                return new OwnerSession(token, identityId, accountId, email);
            } catch (SQLException error) {
                connection.rollback();
                if (error.getMessage() != null && error.getMessage().contains("UNIQUE")) throw new RegistrationException("Email is already registered");
                throw error;
            }
        } catch (SQLException error) { throw new IOException("Unable to register Branch Account", error); }
    }

    synchronized List<App.Event> listEvents(String accountId) throws IOException {
        return readRecords("event_records", accountId, App.Event::fromLine);
    }

    synchronized Optional<String> publicEventAccountId(String eventId) throws IOException {
        String query = "SELECT e.account_id FROM event_records e JOIN branch_accounts a ON a.id = e.account_id "
                + "WHERE e.id = ? AND a.status = 'active'";
        try (Connection connection = open(); PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, eventId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(result.getString("account_id")) : Optional.empty();
            }
        } catch (SQLException error) { throw new IOException("Unable to validate public activity", error); }
    }

    synchronized boolean isPublicEventAvailable(String eventId) throws IOException {
        return publicEventAccountId(eventId).isPresent();
    }

    synchronized void replaceEvents(String accountId, List<App.Event> events) throws IOException {
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            replaceEvents(connection, accountId, events);
            connection.commit();
        } catch (SQLException error) {
            throw new IOException("Unable to save events", error);
        }
    }

    synchronized List<App.Submission> listSubmissions(String accountId) throws IOException {
        return readRecords("submission_records", accountId, App.Submission::fromLine);
    }

    synchronized void replaceSubmissions(String accountId, List<App.Submission> submissions) throws IOException {
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            replaceSubmissions(connection, accountId, submissions);
            connection.commit();
        } catch (SQLException error) {
            throw new IOException("Unable to save submissions", error);
        }
    }

    synchronized List<App.Winner> listWinners(String accountId) throws IOException {
        return readRecords("winner_records", accountId, App.Winner::fromLine);
    }

    synchronized void replaceWinners(String accountId, List<App.Winner> winners) throws IOException {
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            replaceWinners(connection, accountId, winners);
            connection.commit();
        } catch (SQLException error) {
            throw new IOException("Unable to save winners", error);
        }
    }

    synchronized List<App.Operation> listOperations(String accountId) throws IOException {
        return readRecords("operation_records", accountId, App.Operation::fromLine);
    }

    synchronized void replaceOperations(String accountId, List<App.Operation> operations) throws IOException {
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            replaceOperations(connection, accountId, operations);
            connection.commit();
        } catch (SQLException error) {
            throw new IOException("Unable to save operations", error);
        }
    }

    private Connection open() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA journal_mode = WAL");
        }
        return connection;
    }

    private static void createSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS metadata (key TEXT PRIMARY KEY, value TEXT NOT NULL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS branch_accounts (id TEXT PRIMARY KEY, workspace_name TEXT NOT NULL, status TEXT NOT NULL, created_at TEXT NOT NULL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS identities (id TEXT PRIMARY KEY, account_id TEXT REFERENCES branch_accounts(id), role TEXT NOT NULL, email TEXT NOT NULL UNIQUE COLLATE BINARY, password_hash TEXT NOT NULL, created_at TEXT NOT NULL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS sessions (token TEXT PRIMARY KEY, identity_id TEXT NOT NULL REFERENCES identities(id), expires_at TEXT NOT NULL, revoked_at TEXT)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS event_records (id TEXT PRIMARY KEY, account_id TEXT NOT NULL REFERENCES branch_accounts(id), payload TEXT NOT NULL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS submission_records (id TEXT PRIMARY KEY, account_id TEXT NOT NULL REFERENCES branch_accounts(id), payload TEXT NOT NULL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS winner_records (id TEXT PRIMARY KEY, account_id TEXT NOT NULL REFERENCES branch_accounts(id), payload TEXT NOT NULL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS operation_records (id TEXT PRIMARY KEY, account_id TEXT NOT NULL REFERENCES branch_accounts(id), payload TEXT NOT NULL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS account_audits (id TEXT PRIMARY KEY, account_id TEXT REFERENCES branch_accounts(id), actor TEXT NOT NULL, action TEXT NOT NULL, target TEXT NOT NULL, result TEXT NOT NULL, created_at TEXT NOT NULL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS platform_audits (id TEXT PRIMARY KEY, actor TEXT NOT NULL, action TEXT NOT NULL, target TEXT NOT NULL, result TEXT NOT NULL, created_at TEXT NOT NULL)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_event_records_account ON event_records(account_id)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_submission_records_account ON submission_records(account_id)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_winner_records_account ON winner_records(account_id)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_operation_records_account ON operation_records(account_id)");
        }
    }

    private ChinaAccount findChinaAccount(Connection connection) throws SQLException, IOException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT a.id, i.email FROM branch_accounts a JOIN identities i ON i.account_id = a.id WHERE a.workspace_name = ? AND i.role = 'owner'")) {
            statement.setString(1, CHINA_WORKSPACE_NAME);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IOException("Migration marker exists but China Account Owner is missing");
                }
                return new ChinaAccount(result.getString("id"), result.getString("email"));
            }
        }
    }

    private static Optional<String> metadata(Connection connection, String key) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT value FROM metadata WHERE key = ?")) {
            statement.setString(1, key);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(result.getString("value")) : Optional.empty();
            }
        }
    }

    private static void putMetadata(Connection connection, String key, String value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO metadata(key, value) VALUES (?, ?)")) {
            statement.setString(1, key);
            statement.setString(2, value);
            statement.executeUpdate();
        }
    }

    private static List<App.Event> readEvents(Path file) throws IOException {
        List<App.Event> values = new ArrayList<>();
        if (Files.exists(file)) {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) values.add(App.Event.fromLine(line));
            }
        }
        return values;
    }

    private static List<App.Submission> readSubmissions(Path file) throws IOException {
        List<App.Submission> values = new ArrayList<>();
        if (Files.exists(file)) {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) values.add(App.Submission.fromLine(line));
            }
        }
        return values;
    }

    private static List<App.Winner> readWinners(Path file) throws IOException {
        List<App.Winner> values = new ArrayList<>();
        if (Files.exists(file)) {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) values.add(App.Winner.fromLine(line));
            }
        }
        return values;
    }

    private static List<App.Operation> readOperations(Path file) throws IOException {
        List<App.Operation> values = new ArrayList<>();
        if (Files.exists(file)) {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) values.add(App.Operation.fromLine(line));
            }
        }
        return values;
    }

    private static void replaceEvents(Connection connection, String accountId, List<App.Event> values) throws SQLException {
        replaceRecords(connection, "event_records", accountId, values, value -> value.id, App.Event::toLine);
    }

    private static void replaceSubmissions(Connection connection, String accountId, List<App.Submission> values) throws SQLException {
        replaceRecords(connection, "submission_records", accountId, values, value -> value.id, App.Submission::toLine);
    }

    private static void replaceWinners(Connection connection, String accountId, List<App.Winner> values) throws SQLException {
        replaceRecords(connection, "winner_records", accountId, values, value -> value.id, App.Winner::toLine);
    }

    private static void replaceOperations(Connection connection, String accountId, List<App.Operation> values) throws SQLException {
        replaceRecords(connection, "operation_records", accountId, values, value -> value.id, App.Operation::toLine);
    }

    private static <T> void replaceRecords(Connection connection, String table, String accountId, List<T> values, Id<T> id, Payload<T> payload) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement("DELETE FROM " + table + " WHERE account_id = ?")) {
            delete.setString(1, accountId);
            delete.executeUpdate();
        }
        try (PreparedStatement insert = connection.prepareStatement("INSERT INTO " + table + "(id, account_id, payload) VALUES (?, ?, ?)")) {
            for (T value : values) {
                insert.setString(1, id.value(value));
                insert.setString(2, accountId);
                insert.setString(3, payload.value(value));
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private <T> List<T> readRecords(String table, String accountId, Parser<T> parser) throws IOException {
        try (Connection connection = open(); PreparedStatement statement = connection.prepareStatement(
                accountId == null ? "SELECT payload FROM " + table + " ORDER BY rowid" : "SELECT payload FROM " + table + " WHERE account_id = ? ORDER BY rowid")) {
            if (accountId != null) statement.setString(1, accountId);
            try (ResultSet result = statement.executeQuery()) {
                List<T> values = new ArrayList<>();
                while (result.next()) {
                    values.add(parser.parse(result.getString("payload")));
                }
                return values;
            }
        } catch (SQLException error) {
            throw new IOException("Unable to load " + table, error);
        }
    }

    private static void audit(Connection connection, String accountId, String actor, String action, String target, String result) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO account_audits(id, account_id, actor, action, target, result, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, accountId);
            statement.setString(3, actor);
            statement.setString(4, action);
            statement.setString(5, target);
            statement.setString(6, result);
            statement.setString(7, Instant.now().toString());
            statement.executeUpdate();
        }
    }

    private static void platformAudit(Connection connection, String actor, String action, String target, String result) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO platform_audits(id, actor, action, target, result, created_at) VALUES (?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, actor);
            statement.setString(3, action);
            statement.setString(4, target);
            statement.setString(5, result);
            statement.setString(6, Instant.now().toString());
            statement.executeUpdate();
        }
    }

    private static String passwordHash(String password) throws IOException {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] hash = derive(password.toCharArray(), salt);
        return PASSWORD_ITERATIONS + "$" + Base64.getUrlEncoder().withoutPadding().encodeToString(salt) + "$"
                + Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    private static boolean passwordMatches(String password, String stored) throws IOException {
        String[] parts = stored.split("\\$", -1);
        if (parts.length != 3) return false;
        try {
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getUrlDecoder().decode(parts[1]);
            byte[] expected = Base64.getUrlDecoder().decode(parts[2]);
            byte[] actual = derive(password.toCharArray(), salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException error) {
            return false;
        }
    }

    private static byte[] derive(char[] password, byte[] salt) throws IOException {
        return derive(password, salt, PASSWORD_ITERATIONS);
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) throws IOException {
        PBEKeySpec specification = new PBEKeySpec(password, salt, iterations, PASSWORD_KEY_LENGTH);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(specification).getEncoded();
        } catch (InvalidKeySpecException | java.security.NoSuchAlgorithmException error) {
            throw new IOException("Unable to hash password", error);
        } finally {
            specification.clearPassword();
        }
    }

    record ChinaAccount(String accountId, String email) {
    }

    record OwnerSession(String token, String identityId, String accountId, String email) {
    }

    record OwnerSettings(String workspaceName, String email) {
    }

    record PlatformSession(String token, String identityId, String email) {
    }

    record BranchAccountSummary(String id, String workspaceName, String email, String status) {
    }

    static final class RegistrationException extends Exception { RegistrationException(String message) { super(message); } }

    @FunctionalInterface
    private interface Id<T> {
        String value(T value);
    }

    @FunctionalInterface
    private interface Payload<T> {
        String value(T value);
    }

    @FunctionalInterface
    private interface Parser<T> {
        T parse(String value);
    }

    private static boolean validEmail(String value) {
        if (value == null || value.isBlank() || !value.equals(value.trim()) || value.contains(" ")) return false;
        int at = value.indexOf('@'); int dot = value.lastIndexOf('.');
        return at > 0 && dot > at + 1 && dot < value.length() - 1;
    }
}
