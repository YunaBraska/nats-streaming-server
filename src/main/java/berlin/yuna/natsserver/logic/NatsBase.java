package berlin.yuna.natsserver.logic;

import berlin.yuna.clu.logic.SystemUtil;
import berlin.yuna.natsserver.config.NatsStreamingConfig;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static berlin.yuna.clu.logic.SystemUtil.OS;
import static berlin.yuna.clu.model.OsType.OS_WINDOWS;
import static berlin.yuna.natsserver.config.NatsStreamingConfig.ADDR;
import static berlin.yuna.natsserver.config.NatsStreamingConfig.NATS_ARGS;
import static berlin.yuna.natsserver.config.NatsStreamingConfig.NATS_BINARY_PATH;
import static berlin.yuna.natsserver.config.NatsStreamingConfig.NATS_CONFIG_FILE;
import static berlin.yuna.natsserver.config.NatsStreamingConfig.NATS_DOWNLOAD_URL;
import static berlin.yuna.natsserver.config.NatsStreamingConfig.NATS_LOG_NAME;
import static berlin.yuna.natsserver.config.NatsStreamingConfig.NATS_SYSTEM;
import static berlin.yuna.natsserver.config.NatsStreamingConfig.PID;
import static berlin.yuna.natsserver.config.NatsStreamingConfig.PORT;
import static berlin.yuna.natsserver.logic.NatsUtils.download;
import static berlin.yuna.natsserver.logic.NatsUtils.getEnv;
import static berlin.yuna.natsserver.logic.NatsUtils.getNextFreePort;
import static berlin.yuna.natsserver.logic.NatsUtils.isEmpty;
import static berlin.yuna.natsserver.logic.NatsUtils.removeQuotes;
import static berlin.yuna.natsserver.logic.NatsUtils.resolveEnvs;
import static berlin.yuna.natsserver.logic.NatsUtils.unzip;
import static java.lang.Integer.parseInt;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_READ;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_WRITE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static java.util.Arrays.stream;
import static java.util.logging.Logger.getLogger;

/**
 * {@link NatsBase}
 *
 * @author Yuna Morgenstern
 * @see SystemUtil
 * @see NatsBase
 * @since 1.0
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public abstract class NatsBase implements AutoCloseable {

    public static final String NATS_PREFIX = "NATS_";

    final String name;
    final Logger logger;
    final Map<NatsStreamingConfig, String> config = new ConcurrentHashMap<>();
    final List<String> customArgs = new ArrayList<>();
    Process process;
    private static final String TMP_DIR = "java.io.tmpdir";

    NatsBase(final List<String> customArgs) {
        setDefaultConfig().setEnvConfig().readPropertyFile();
        this.name = getValue(NATS_LOG_NAME);
        this.logger = getLogger(name);
        this.customArgs.addAll(customArgs);
    }

    /**
     * get process id
     *
     * @return process id or -1 if process is not running
     */
    public int pid() {
        try {
            return Integer.parseInt(String.join(" ", Files.readAllLines(pidFile(), StandardCharsets.UTF_8)).trim());
        } catch (IOException e) {
            return -1;
        }
    }

    /**
     * get process id file which only exists when the process is running
     *
     * @return process id file path
     */
    public Path pidFile() {
        return Paths.get(getValue(PID, () -> Paths.get(
                getEnv(TMP_DIR),
                getValue(NATS_LOG_NAME).toLowerCase(),
                port() + ".pid"
        ).toString()));
    }

    /**
     * nats binary file which only exists after download or if its already placed there
     *
     * @return nats binary file path
     */
    public Path binaryFile() {
        return Paths.get(getValue(NATS_BINARY_PATH, () -> Paths.get(
                getEnv(TMP_DIR),
                getValue(NATS_LOG_NAME).toLowerCase(),
                getValue(NATS_LOG_NAME).toLowerCase() + "_" + getValue(NATS_SYSTEM) + (OS == OS_WINDOWS ? ".exe" : "")
        ).toString()));
    }

    /**
     * nats download url which is usually a zip file
     *
     * @return nats download url
     */
    public String downloadUrl() {
        return getValue(NATS_DOWNLOAD_URL);
    }

    /**
     * nats server URL from bind to host address
     *
     * @return nats server url
     */
    public String url() {
        return "nats://" + getValue(ADDR) + ":" + port();
    }

    /**
     * Gets the port out of the configuration
     *
     * @return configured port of the server
     * @throws RuntimeException when the port is not configured
     */
    public int port() {
        return parseInt(getValue(PORT));
    }

    /**
     * Adds custom arguments to the nats start command
     *
     * @param args arguments
     * @return self {@link NatsBase}
     */
    public NatsBase args(final String... args) {
        customArgs.addAll(Arrays.asList(args));
        return this;
    }

    /**
     * Get customArguments
     *
     * @return list of custom arguments
     */
    public List<String> args() {
        return customArgs;
    }

    /**
     * Gets resolved config value from key
     *
     * @param key config key
     * @return config key value
     */
    public String getValue(final NatsStreamingConfig key) {
        return getValue(key, () -> key.valueRaw() == null ? null : String.valueOf(key.valueRaw()));
    }

    /**
     * Gets resolved config value from key
     *
     * @param key config key
     * @param or  lazy loaded fallback value
     * @return config key value
     */
    public String getValue(final NatsStreamingConfig key, final Supplier<String> or) {
        return resolveEnvs(Optional.ofNullable(config.get(key)).orElseGet(or), config);
    }

    NatsBase deletePidFile() {
        try {
            Files.deleteIfExists(pidFile());
        } catch (IOException ignored) {
            //ignored
        }
        return this;
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "java:S899"})
    Path downloadNats() throws IOException {
        final Path binaryPath = binaryFile();
        Files.createDirectories(binaryPath.getParent());
        if (Files.notExists(binaryPath)) {
            final URL source = new URL(downloadUrl());
            unzip(download(source, Paths.get(binaryFile().toString() + ".zip")), binaryPath);
        }
        binaryPath.toFile().setExecutable(true);
        SystemUtil.setFilePermissions(binaryPath, OWNER_EXECUTE, OTHERS_EXECUTE, OWNER_READ, OTHERS_READ, OWNER_WRITE, OTHERS_WRITE);
        return binaryPath;
    }

    String prepareCommand() {
        final StringBuilder command = new StringBuilder();
        final Map<NatsStreamingConfig, String> finalConfig = resolveConfig();
        command.append(binaryFile().toString());
        finalConfig.forEach((cfg, value) -> {
            if (!cfg.name().startsWith(NATS_PREFIX) && !isEmpty(value)) {
                command.append(" ");
                command.append(cfg.key());
                if (!cfg.desc().startsWith("[/]")) {
                    command.append(value.trim().toLowerCase());
                }
            }
        });
        command.append(customArgs.stream().collect(Collectors.joining(" ", " ", "")));
        command.append(stream(getValue(NATS_ARGS, () -> "").split("\\,")).map(String::trim).collect(Collectors.joining(" ", " ", "")));
        return command.toString();
    }

    NatsBase setNextFreePort() {
        if (getValue(PORT, () -> "-1").equals("-1")) {
            config.put(PORT, "" + getNextFreePort((int) PORT.valueRaw()));
        }
        return this;
    }

    private Map<NatsStreamingConfig, String> resolveConfig() {
        final Map<NatsStreamingConfig, String> finalConfig = new EnumMap<>(this.config);
        finalConfig.computeIfAbsent(PID, value -> pidFile().toString());
        stream(NatsStreamingConfig.values()).forEach(key -> finalConfig.computeIfAbsent(key, value -> getValue(key)));
        return finalConfig;
    }

    private NatsBase readPropertyFile() {
        if (config.containsKey(NATS_CONFIG_FILE)) {
            final Properties prop = new Properties();
            try (final InputStream inputStream = new FileInputStream(getValue(NATS_CONFIG_FILE))) {
                prop.load(inputStream);
            } catch (IOException e) {
                getLogger(getValue(NATS_LOG_NAME)).severe("Unable to read property file [" + e.getMessage() + "]");
            }
            prop.forEach((key, value) -> config.put(NatsStreamingConfig.valueOf((String) key), removeQuotes((String) value)));
        }
        return this;
    }

    private NatsBase setDefaultConfig() {
        for (NatsStreamingConfig cfg : NatsStreamingConfig.values()) {
            final String value = cfg.value();
            if (value != null) {
                config.put(cfg, value);
            }
        }
        config.put(NATS_SYSTEM, NatsUtils.getSystem());
        return this;
    }

    private NatsBase setEnvConfig() {
        for (NatsStreamingConfig cfg : NatsStreamingConfig.values()) {
            final String value = getEnv(cfg.name().startsWith(NATS_PREFIX) ? cfg.name() : NATS_PREFIX + cfg.name());
            if (value != null) {
                config.put(cfg, value);
            }
        }
        return this;
    }

    @Override
    public String toString() {
        return "NatsBase{" +
                "name=" + name +
                ", pid='" + pid() + '\'' +
                ", port=" + port() +
                ", configs=" + config.size() +
                ", customArgs=" + customArgs.size() +
                '}';
    }
}