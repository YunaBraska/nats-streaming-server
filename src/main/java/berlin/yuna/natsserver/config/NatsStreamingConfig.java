package berlin.yuna.natsserver.config;

import berlin.yuna.natsserver.logic.NatsStreaming;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public enum NatsStreamingConfig {

    //Streaming Server Options
    CLUSTER_ID("--cluster_id", null, String.class, "Cluster ID (default: test-cluster)"),
    STORE("--store", null, String.class, "Store type: MEMORY|FILE|SQL (default: MEMORY)"),
    DIR("--dir", null, String.class, "For FILE store type, this is the root directory"),
    MAX_CHANNELS("--max_channels", null, Integer.class, "Max number of channels (0 for unlimited)"),
    MAX_SUBS("--max_subs", null, Integer.class, "Max number of subscriptions per channel (0 for unlimited)"),
    MAX_MSGS("--max_msgs", null, Integer.class, "Max number of messages per channel (0 for unlimited)"),
    MAX_BYTES("--max_bytes", null, Integer.class, "Max messages total size per channel (0 for unlimited)"),
    MAX_AGE("--max_age", null, String.class, "Max duration a message can be stored (\"0s\" for unlimited)"),
    MAX_INACTIVITY("--max_inactivity", "0", String.class, "Max inactivity (no new message, no subscription) after which a channel can be garbage collected (0 for unlimited)"),
    NATS_SERVER("--nats_server", null, String.class, "Connect to this external NATS Server URL (embedded otherwise)"),
    STAN_CONFIG("--stan_config", null, String.class, "Streaming server configuration file"),
    HB_INTERVAL("--hb_interval", null, String.class, "Interval at which server sends heartbeat to a client"),
    HB_TIMEOUT("--hb_timeout", null, String.class, "How long server waits for a heartbeat response"),
    HB_FAIL_COUNT("--hb_fail_count", 3, Integer.class, "Number of failed heartbeats before server closes the client connection"),
    FT_GROUP("--ft_group", null, String.class, "Name of the FT Group. A group can be 2 or more servers with a single active server and all sharing the same datastore"),
    SIGNAL("--signal", null, String.class, "Send signal to nats-streaming-server process (stop, quit, reopen, reload - only for embedded NATS Server)"),
    ENCRYPT("--encrypt", null, Boolean.class, "Specify if server should use encryption at rest"),
    ENCRYPTION_CIPHER("--encryption_cipher", null, String.class, "Cipher to use for encryption. Currently support AES and CHAHA (ChaChaPoly). Defaults to AES"),
    ENCRYPTION_KEY("--encryption_key", null, String.class, "Encryption Key. It is recommended to specify it through the NATS_STREAMING_ENCRYPTION_KEY environment variable instead"),
    REPLACE_DURABLE("--replace_durable", null, Boolean.class, "Replace the existing durable subscription instead of reporting a duplicate durable error"),

    //Streaming Server Clustering Options
    CLUSTERED("--clustered", null, Boolean.class, "Run the server in a clustered configuration (default: false)"),
    CLUSTER_NODE_ID("--cluster_node_id", "82837aa8-b23d-481d-a07b-910a14fd0385", String.class, "ID of the node within the cluster if there is no stored ID (default: random UUID)"),
    CLUSTER_BOOTSTRAP("--cluster_bootstrap", null, Boolean.class, "Bootstrap the cluster if there is no existing state by electing self as leader (default: false)"),
    CLUSTER_PEERS("--cluster_peers", null, String.class, "Comma separated list of cluster peer node IDs to bootstrap cluster state"),
    CLUSTER_LOG_PATH("--cluster_log_path", null, String.class, "Directory to store log replication data"),
    CLUSTER_LOG_CACHE_SIZE("--cluster_log_cache_size", null, Integer.class, "Number of log entries to cache in memory to reduce disk IO (default: 512)"),
    CLUSTER_LOG_SNAPSHOTS("--cluster_log_snapshots", null, Integer.class, "Number of log snapshots to retain (default: 2)"),
    CLUSTER_TRAILING_LOGS("--cluster_trailing_logs", null, Integer.class, "Number of log entries to leave after a snapshot and compaction"),
    CLUSTER_SYNC("--cluster_sync", null, Boolean.class, "Do a file sync after every write to the replication log and message store"),
    CLUSTER_RAFT_LOGGING("--cluster_raft_logging", null, Boolean.class, "Enable logging from the Raft library (disabled by default)"),
    CLUSTER_ALLOW_ADD_REMOVE_NODE("--cluster_allow_add_remove_node", null, Boolean.class, "Enable the ability to send NATS requests to the leader to add/remove cluster nodes"),

    //Streaming Server File Store Options
    FILE_COMPACT_ENABLED("--file_compact_enabled", null, Boolean.class, "Enable file compaction"),
    FILE_COMPACT_FRAG("--file_compact_frag", null, Integer.class, "File fragmentation threshold for compaction"),
    FILE_COMPACT_INTERVAL("--file_compact_interval", null, Integer.class, "Minimum interval (in seconds) between file compactions"),
    FILE_COMPACT_MIN_SIZE("--file_compact_min_size", null, Integer.class, "Minimum file size for compaction"),
    FILE_BUFFER_SIZE("--file_buffer_size", null, Integer.class, "File buffer size (in bytes)"),
    FILE_CRC("--file_crc", null, Boolean.class, "Enable file CRC-32 checksum"),
    FILE_CRC_POLY("--file_crc_poly", null, Integer.class, "Polynomial used to make the table used for CRC-32 checksum"),
    FILE_SYNC("--file_sync", null, Boolean.class, "Enable File.Sync on Flush"),
    FILE_SLICE_MAX_MSGS("--file_slice_max_msgs", null, Integer.class, "Maximum number of messages per file slice (subject to channel limits)"),
    FILE_SLICE_MAX_BYTES("--file_slice_max_bytes", null, Integer.class, "Maximum file slice size - including index file (subject to channel limits)"),
    FILE_SLICE_MAX_AGE("--file_slice_max_age", null, String.class, "Maximum file slice duration starting when the first message is stored (subject to channel limits)"),
    FILE_SLICE_ARCHIVE_SCRIPT("--file_slice_archive_script", null, String.class, "Path to script to use if you want to archive a file slice being removed"),
    FILE_FDS_LIMIT("--file_fds_limit", null, Integer.class, "Store will try to use no more file descriptors than this given limit"),
    FILE_PARALLEL_RECOVERY("--file_parallel_recovery", null, Integer.class, "On startup, number of channels that can be recovered in parallel"),
    FILE_TRUNCATE_BAD_EOF("--file_truncate_bad_eof", null, Boolean.class, "Truncate files for which there is an unexpected EOF on recovery, dataloss may occur"),
    FILE_READ_BUFFER_SIZE("--file_read_buffer_size", null, Integer.class, "Size of messages read ahead buffer (0 to disable)"),
    FILE_AUTO_SYNC("--file_auto_sync", null, String.class, "Interval at which the store should be automatically flushed and sync'ed on disk (<= 0 to disable)"),

    //Streaming Server SQL Store Options
    SQL_DRIVER("--sql_driver", null, String.class, "Name of the SQL Driver (\"mysql\" or \"postgres\")"),
    SQL_SOURCE("--sql_source", null, String.class, "Datasource used when opening an SQL connection to the database"),
    SQL_NO_CACHING("--sql_no_caching", null, Boolean.class, "Enable/Disable caching for improved performance"),
    SQL_MAX_OPEN_CONNS("--sql_max_open_conns", null, Integer.class, "Maximum number of opened connections to the database"),
    SQL_BULK_INSERT_LIMIT("--sql_bulk_insert_limit", null, Integer.class, "Maximum number of messages stored with a single SQL \"INSERT\" statement"),

    //Streaming Server TLS Options
    SECURE("-secure", null, Boolean.class, "Use a TLS connection to the NATS server without"),
    TLS_CLIENT_KEY("-tls_client_key", null, String.class, "Client key for the streaming server"),
    TLS_CLIENT_CERT("-tls_client_cert", null, String.class, "Client certificate for the streaming server"),
    TLS_CLIENT_CACERT("-tls_client_cacert", null, String.class, "Client certificate CA for the streaming server"),

    //Streaming Server Logging Options
    STAN_DEBUG("--stan_debug", null, Boolean.class, "Enable STAN debugging output"),
    STAN_TRACE("--stan_trace", null, Boolean.class, "Trace the raw STAN protocol"),
    SDV("-SDV", null, SilentBoolean.class, "Debug and trace STAN"),
    DV("-DV", null, SilentBoolean.class, "Debug and trace"),

    //NATS Server Options
    ADDR("--addr", "0.0.0.0", String.class, "Bind to host address (default: 0.0.0.0)"),
    PORT("--port", 4222, Integer.class, "Use port for clients (default: 4222)"),
    PID("--pid", null, String.class, "File to store PID"),
    HTTP_PORT("--http_port", null, Integer.class, "Use port for http monitoring"),
    HTTPS_PORT("--https_port", null, Integer.class, "Use port for https monitoring"),
    CONFIG("--config", null, String.class, "Configuration file"),

    //NATS Server Logging Options
    LOG("--log", null, String.class, "File to redirect log output"),
    LOGTIME("--logtime", null, Boolean.class, "Timestamp log entries (default: true)"),
    SYSLOG("--syslog", null, Boolean.class, "Enable syslog as log method"),
    SYSLOG_NAME("--syslog_name", null, String.class, "On Windows, when running several servers as a service, use this name for the event source"),
    REMOTE_SYSLOG("--remote_syslog", null, String.class, "Syslog server addr (udp://localhost:514)"),
    DEBUG("--debug", null, SilentBoolean.class, "Enable debugging output"),
    TRACE("--trace", null, SilentBoolean.class, "Trace the raw protocol"),

    //NATS Server Authorization Options
    USER("--user", null, String.class, "User required for connections"),
    PASS("--pass", null, String.class, "Password required for connections"),
    AUTH("--auth", null, String.class, "Authorization token required for connections"),

    //TLS Options
    TLS("--tls", null, Boolean.class, "Enable TLS, do not verify clients (default: false)"),
    TLSCERT("--tlscert", null, String.class, "Server certificate file"),
    TLSKEY("--tlskey", null, String.class, "Private key for server certificate"),
    TLSVERIFY("--tlsverify", null, Boolean.class, "Enable TLS, verify client certificates"),
    TLSCACERT("--tlscacert", null, String.class, "Client certificate CA for verification"),

    //NATS Clustering Options
    ROUTES("--routes", null, String.class, "Routes to solicit and connect"),
    CLUSTER("--cluster", null, String.class, "Cluster URL for solicited routes"),
    // Common Options
    HELP("--help", null, SilentBoolean.class, "Show this message" + System.lineSeparator() + "(default: false)"),
    HELP_TLS("--help_tls", null, SilentBoolean.class, "TLS help." + System.lineSeparator() + "(default: false)"),

    //WRAPPER configs
    NATS_AUTOSTART(null, true, Boolean.class, "[true] == auto closable, [false] == manual use `.start()` method (default: true)"),
    NATS_SHUTDOWN_HOOK(null, true, Boolean.class, "[true] == registers a shutdown hook, [false] == manual use `.stop()` method (default: true)"),
    NATS_LOG_LEVEL(null, null, String.class, "java log level e.g. [OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL]"),
    NATS_TIMEOUT_MS(null, 10000, String.class, "true = auto closable, false manual use `.start()` method"),
    NATS_SYSTEM(null, null, String.class, "suffix for binary path"),
    NATS_LOG_NAME(null, NatsStreaming.class.getSimpleName(), String.class, "java wrapper name"),
    NATS_STREAMING_VERSION(null, "v0.25.6", String.class, "Overwrites Nats server version on path"),
    NATS_DOWNLOAD_URL(null, "https://github.com/nats-io/nats-streaming-server/releases/download/%" + NATS_STREAMING_VERSION.name() + "%/nats-streaming-server-%" + NATS_STREAMING_VERSION.name() + "%-%" + NATS_SYSTEM.name() + "%.zip", String.class, "Path to Nats binary or zip file"),
    NATS_BINARY_PATH(null, null, String.class, "Target Path to Nats binary or zip file - auto from " + NATS_DOWNLOAD_URL.name() + ""),
    NATS_PROPERTY_FILE(null, null, String.class, "Additional config file (properties / KV) same as DSL configs"),
    NATS_ARGS(null, null, String.class, "custom arguments separated by &&");

    public static final String ARGS_SEPARATOR = "&&";
    public static final Level[] ALL_LOG_LEVEL = new Level[]{Level.OFF, Level.SEVERE, Level.WARNING, Level.INFO, Level.CONFIG, Level.FINE, Level.FINER, Level.FINEST, Level.ALL};

    public static Level logLevelOf(final String level) {
        return Arrays.stream(ALL_LOG_LEVEL).filter(value -> value.getName().equalsIgnoreCase(level)).findFirst().orElse(null);
    }

    private final String key;
    private final Class<?> type;
    private final Object defaultValue;
    private final String description;

    NatsStreamingConfig(final String key, final Object defaultValue, final Class<?> type, final String description) {
        this.key = key;
        this.type = type;
        this.defaultValue = defaultValue;
        this.description = description;
    }

    public boolean isWritableValue() {
        return type != SilentBoolean.class;
    }


    public Object defaultValue() {
        return defaultValue;
    }

    public String description() {
        return description;
    }

    /**
     * @return value as string
     */
    public String defaultValueStr() {
        return defaultValue == null ? null : defaultValue.toString();
    }

    /**
     * Command line property key
     *
     * @return key for command line
     */
    public String key() {
        return key;
    }

    public Class<?> type() {
        return type;
    }

    @SuppressWarnings("java:S2094")
    public static class SilentBoolean extends AtomicBoolean {
        //DUMMY CLASS
    }
}
