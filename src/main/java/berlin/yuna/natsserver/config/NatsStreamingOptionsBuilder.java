package berlin.yuna.natsserver.config;

import berlin.yuna.natsserver.logic.NatsStreaming;
import berlin.yuna.natsserver.logic.NatsUtils;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import static berlin.yuna.natsserver.config.NatsStreamingConfig.ARGS_SEPARATOR;
import static java.util.Optional.ofNullable;

@SuppressWarnings({"UnusedReturnValue", "SameParameterValue"})
public class NatsStreamingOptionsBuilder {

    protected Logger logger;
    protected Map<NatsStreamingConfig, String> configMap = new EnumMap<>(NatsStreamingConfig.class);

    protected NatsStreamingOptionsBuilder() {
    }

    /**
     * @return immutable config for {@link NatsStreaming}
     */
    public NatsStreamingOptions build() {
        return new NatsStreamingOptions(logger, configMap);
    }

    /**
     * @return {@link NatsStreaming} build nats server from config
     */
    public NatsStreaming nats() {
        return new NatsStreaming(this);
    }

    /**
     * @return Nats version
     * @see NatsStreamingConfig#NATS_STREAMING_VERSION
     */
    public String version() {
        return configMap.get(NatsStreamingConfig.NATS_STREAMING_VERSION);
    }

    /**
     * @param version Sets the nats version
     * @return self {@link NatsStreamingOptionsBuilder}
     * @see NatsStreamingConfig#NATS_STREAMING_VERSION
     */
    public NatsStreamingOptionsBuilder version(final String version) {
        configMap.put(
                NatsStreamingConfig.NATS_STREAMING_VERSION,
                ofNullable(version).filter(NatsUtils::isNotEmpty).map(v -> v.toLowerCase().startsWith("v") ? v : "v" + v).orElse(null)
        );
        return this;
    }

    /**
     * @param version Sets the nats version
     * @return self {@link NatsStreamingOptionsBuilder}
     * @see NatsStreamingConfig#NATS_STREAMING_VERSION
     */
    public NatsStreamingOptionsBuilder version(final NatsStreamingVersion version) {
        configMap.put(NatsStreamingConfig.NATS_STREAMING_VERSION, version != null ? version.value() : null);
        return this;
    }

    /**
     * @return The port to start on or &lt;=0 to use an automatically allocated port
     * @see NatsStreamingConfig#PORT
     */
    public Integer port() {
        return getValueI(configMap, NatsStreamingConfig.PORT);
    }

    /**
     * @param port The port to start on or &lt;=0 to use an automatically allocated port
     * @return self {@link NatsStreamingOptionsBuilder}
     * @see NatsStreamingConfig#PORT
     */
    public NatsStreamingOptionsBuilder port(final Integer port) {
        setValueI(configMap, NatsStreamingConfig.PORT, port);
        return this;
    }

    /**
     * @return true if debug is enabled
     * @see NatsStreamingConfig#DEBUG
     */
    public Boolean debug() {
        return getValueB(configMap, NatsStreamingConfig.DEBUG);
    }

    /**
     * @param debug whether to start the server with the debug flag
     * @return self {@link NatsStreamingOptionsBuilder}
     * @see NatsStreamingConfig#DEBUG
     */
    public NatsStreamingOptionsBuilder debug(final Boolean debug) {
        setValueB(configMap, NatsStreamingConfig.DEBUG, debug);
        return this;
    }

    /**
     * @return path to a custom config file
     * @see NatsStreamingConfig#CONFIG
     */
    public Path configFile() {
        return getValue(configMap, Path::of, NatsStreamingConfig.CONFIG);
    }

    /**
     * @param configFile path to a custom config file
     * @return self {@link NatsStreamingOptionsBuilder}
     * @see NatsStreamingConfig#CONFIG
     */
    public NatsStreamingOptionsBuilder configFile(final Path configFile) {
        setValue(configMap, Path::toString, NatsStreamingConfig.CONFIG, configFile);
        return this;
    }

    /**
     * @return path to a custom config property file
     * @see NatsStreamingConfig#NATS_PROPERTY_FILE
     */
    public Path configPropertyFile() {
        return getValue(configMap, Path::of, NatsStreamingConfig.NATS_PROPERTY_FILE);
    }

    /**
     * @param configFile path to a custom config property file
     * @return self {@link NatsStreamingOptionsBuilder}
     * @see NatsStreamingConfig#NATS_PROPERTY_FILE
     */
    public NatsStreamingOptionsBuilder configPropertyFile(final Path configFile) {
        setValue(configMap, Path::toString, NatsStreamingConfig.NATS_PROPERTY_FILE, configFile);
        return this;
    }

    /**
     * @return custom args to add to the command line
     * @see NatsStreamingConfig#NATS_ARGS
     */
    public String[] customArgs() {
        return getValue(configMap, args -> args.split(ARGS_SEPARATOR), NatsStreamingConfig.NATS_ARGS);
    }

    /**
     * @param customArgs custom args to set
     * @return self {@link NatsStreamingOptionsBuilder}
     * @see NatsStreamingConfig#NATS_ARGS
     */
    public NatsStreamingOptionsBuilder customArgs(final String... customArgs) {
        ofNullable(customArgs).ifPresent(value -> configMap.put(NatsStreamingConfig.NATS_ARGS, String.join(ARGS_SEPARATOR, value)));
        return this;
    }

    /**
     * @param customArgs custom args to add
     * @return self {@link NatsStreamingOptionsBuilder}
     * @see NatsStreamingConfig#NATS_ARGS
     */
    public NatsStreamingOptionsBuilder addArgs(final String... customArgs) {
        final var args = configMap.get(NatsStreamingConfig.NATS_ARGS);
        ofNullable(customArgs).ifPresent(value -> {
            if (args != null) {
                configMap.put(NatsStreamingConfig.NATS_ARGS, String.join(ARGS_SEPARATOR, args, String.join(ARGS_SEPARATOR, value)));
            } else {
                configMap.put(NatsStreamingConfig.NATS_ARGS, String.join(ARGS_SEPARATOR, value));
            }
        });
        return this;
    }

    /**
     * @return custom logger
     */
    public Logger logger() {
        return logger;
    }

    /**
     * @param logger custom logger
     * @return self {@link NatsStreamingOptionsBuilder}
     */
    public NatsStreamingOptionsBuilder logger(final Logger logger) {
        this.logger = logger;
        return this;
    }


    /**
     * @return custom LogLevel
     */
    public Level logLevel() {
        return getValue(configMap, NatsStreamingConfig::logLevelOf, NatsStreamingConfig.NATS_LOG_LEVEL);
    }

    /**
     * @param level custom logLevel
     * @return self {@link NatsStreamingOptionsBuilder}
     */
    public NatsStreamingOptionsBuilder logLevel(final Level level) {
        setValue(configMap, Level::getName, NatsStreamingConfig.NATS_LOG_LEVEL, level);
        return this;
    }

    /**
     * @return true = auto closable, false manual use `.start()` method
     * @see NatsStreamingConfig#NATS_AUTOSTART
     */
    public Boolean autostart() {
        return getValueB(configMap, NatsStreamingConfig.NATS_AUTOSTART);
    }

    /**
     * @param autostart true = auto closable, false manual use `.start()` method
     * @return self {@link NatsStreamingOptionsBuilder}
     * @see NatsStreamingConfig#NATS_AUTOSTART
     */
    public NatsStreamingOptionsBuilder autostart(final Boolean autostart) {
        setValueB(configMap, NatsStreamingConfig.NATS_AUTOSTART, autostart);
        return this;
    }

    /**
     * @return true = registers a shutdown hook, false manual use `.stop()` method
     * @see NatsStreamingConfig#NATS_SHUTDOWN_HOOK
     */
    public Boolean shutdownHook() {
        return getValueB(configMap, NatsStreamingConfig.NATS_SHUTDOWN_HOOK);
    }

    /**
     * @param enabled true = registers a shutdown hook, false manual use `.stop()` method
     * @return self {@link NatsStreamingOptionsBuilder}
     * @see NatsStreamingConfig#NATS_SHUTDOWN_HOOK
     */
    public NatsStreamingOptionsBuilder shutdownHook(final Boolean enabled) {
        setValueB(configMap, NatsStreamingConfig.NATS_SHUTDOWN_HOOK, enabled);
        return this;
    }

    /**
     * @return defines the start-up timeout in milliseconds (-1 == default)
     */
    public Long timeoutMs() {
        return getValue(configMap, Long::parseLong, NatsStreamingConfig.NATS_TIMEOUT_MS);
    }

    /**
     * @param timeoutMs defines the start-up timeout in milliseconds (-1 == default)
     * @return self {@link NatsStreamingOptionsBuilder}
     * @see NatsStreamingConfig#NATS_TIMEOUT_MS
     */
    public NatsStreamingOptionsBuilder timeoutMs(final Number timeoutMs) {
        setValue(configMap, number -> String.valueOf(number.longValue()), NatsStreamingConfig.NATS_TIMEOUT_MS, timeoutMs);
        return this;
    }

    /**
     * @return configMap
     * @see NatsStreamingConfig
     */
    public Map<NatsStreamingConfig, String> configMap() {
        return configMap;
    }

    /**
     * configMap
     *
     * @return self {@link NatsStreamingOptionsBuilder}
     * @see NatsStreamingConfig
     */
    public NatsStreamingOptionsBuilder configMap(final Map<NatsStreamingConfig, String> configMap) {
        this.configMap = new EnumMap<>(configMap);
        return this;
    }

    /**
     * Adds additional {@link NatsStreamingConfig} <br />
     *
     * @param key   example: {@link NatsStreamingConfig#PORT}
     * @param value example: "4222"
     * @return self {@link NatsStreamingOptionsBuilder}
     * @see NatsStreamingConfig
     */
    public NatsStreamingOptionsBuilder config(final NatsStreamingConfig key, final String value) {
        configMap.put(key, value);
        return this;
    }

    /**
     * Adds additional {@link NatsStreamingConfig} <br />
     * The Key is caseInsensitive. Key doesn't need to have the prefix '-' or '--'
     *
     * @param kv example: port, 4222, user, admin, password, admin
     * @return self {@link NatsStreamingOptionsBuilder}
     */
    public NatsStreamingOptionsBuilder config(final String... kv) {
        for (int i = 0; i < kv.length - 1; i += 2) {
            config(NatsStreamingConfig.valueOf(kv[i].toUpperCase().replace("-", "")), kv[i + 1]);
        }
        return this;
    }

    protected static Integer getValueI(final Map<NatsStreamingConfig, String> config, final NatsStreamingConfig key) {
        return getValue(config, Integer::parseInt, key);
    }

    protected static Boolean getValueB(final Map<NatsStreamingConfig, String> config, final NatsStreamingConfig key) {
        return getValue(config, Boolean::parseBoolean, key);
    }

    protected static <T> T getValue(final Map<NatsStreamingConfig, String> config, final Function<String, T> map, final NatsStreamingConfig key) {
        return ofNullable(config.get(key)).map(map).orElse(null);
    }

    protected static void setValueI(final Map<NatsStreamingConfig, String> config, final NatsStreamingConfig key, final Integer value) {
        setValue(config, Object::toString, key, value);
    }

    protected static void setValueB(final Map<NatsStreamingConfig, String> config, final NatsStreamingConfig key, final Boolean value) {
        setValue(config, Object::toString, key, value);
    }

    protected static <T> void setValue(final Map<NatsStreamingConfig, String> config, final Function<T, String> map, final NatsStreamingConfig key, final T value) {
        ofNullable(value).map(map).ifPresent(val -> config.put(key, val));
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final NatsStreamingOptionsBuilder that = (NatsStreamingOptionsBuilder) o;
        return Objects.equals(logger, that.logger) && Objects.equals(configMap, that.configMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(logger, configMap);
    }
}
