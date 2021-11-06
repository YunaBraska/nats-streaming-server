package berlin.yuna.natsserver.logic;

import berlin.yuna.clu.logic.SystemUtil;
import berlin.yuna.clu.logic.Terminal;
import berlin.yuna.clu.model.OsArch;
import berlin.yuna.clu.model.OsArchType;
import berlin.yuna.clu.model.OsType;
import berlin.yuna.natsserver.config.NatsStreamingConfig;
import berlin.yuna.natsserver.config.NatsStreamingSourceConfig;
import berlin.yuna.natsserver.model.exception.NatsFileReaderException;
import berlin.yuna.natsserver.model.exception.NatsStartException;
import berlin.yuna.natsserver.model.exception.NatsStreamingDownloadException;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.BindException;
import java.net.ConnectException;
import java.net.PortUnreachableException;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.MissingFormatArgumentException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static berlin.yuna.clu.logic.SystemUtil.OS;
import static berlin.yuna.clu.logic.SystemUtil.OS_ARCH;
import static berlin.yuna.clu.logic.SystemUtil.OS_ARCH_TYPE;
import static berlin.yuna.clu.model.OsType.OS_WINDOWS;
import static berlin.yuna.natsserver.config.NatsStreamingConfig.PID;
import static berlin.yuna.natsserver.config.NatsStreamingConfig.PORT;
import static berlin.yuna.natsserver.config.NatsStreamingConfig.SIGNAL;
import static java.nio.channels.Channels.newChannel;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_READ;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_WRITE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static java.util.Comparator.comparingLong;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * {@link NatsStreaming}
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class NatsStreaming {

    /**
     * simpleName from {@link NatsStreaming} class
     */
    protected int pid = -1;
    protected final String name;
    protected static final Logger LOG = getLogger(NatsStreaming.class);
    protected static final String TMP_DIR = System.getProperty("java.io.tmpdir");

    private Process process;
    private String source = NatsStreamingSourceConfig.URL.getDefaultValue(OS, OS_ARCH, OS_ARCH_TYPE);
    private Map<NatsStreamingConfig, String> config = getDefaultConfig();

    /**
     * Create {@link NatsStreaming} without any start able configuration
     */
    public NatsStreaming() {
        name = NatsStreaming.class.getSimpleName() + "server";
    }

    /**
     * Create {@link NatsStreaming} with simplest start able configuration
     *
     * @param port {@code -1} for random port
     */
    public NatsStreaming(int port) {
        this();
        port(port);
    }

    /**
     * Create custom {@link NatsStreaming} with simplest configuration {@link NatsStreaming#config(String...)}
     *
     * @param config passes the original parameters to the server. example: port:4222, user:admin, password:admin
     */
    public NatsStreaming(final String... config) {
        this();
        this.config(config);
    }

    /**
     * GetNatServerConfig
     *
     * @return the {@link NatsStreaming} configuration
     */
    public Map<NatsStreamingConfig, String> config() {
        return config;
    }

    /**
     * Sets a single config value
     *
     * @return the {@link NatsStreaming} configuration
     */
    public NatsStreaming config(final NatsStreamingConfig key, final String value) {
        config.remove(key, value);
        config.put(key, value);
        return this;
    }

    /**
     * Passes the original parameters to the server on startup
     *
     * @param config passes the original parameters to the server.
     * @return {@link NatsStreaming}
     * @see NatsStreaming#config(String...)
     * @see NatsStreamingConfig
     */
    public NatsStreaming config(final Map<NatsStreamingConfig, String> config) {
        this.config = config;
        return this;
    }

    /**
     * Passes the original parameters to the server on startup
     *
     * @param config example: port:4222, user:admin, password:admin
     * @return {@link NatsStreaming}
     * @see NatsStreamingConfig
     */
    public NatsStreaming config(final String... config) {
        for (String property : config) {
            String[] pair = property.split(":");
            if (isEmpty(property) || pair.length != 2) {
                LOG.error("Could not parse property [{}] pair length [{}]", property, pair.length);
                continue;
            }
            config(NatsStreamingConfig.valueOf(pair[0].toUpperCase().replace("-", "")), pair[1]);
        }
        return this;
    }

    /**
     * Starts the server in {@link ProcessBuilder} with the given parameterConfig {@link NatsStreaming#config(String...)}
     * Throws all exceptions as {@link RuntimeException}
     *
     * @return {@link NatsStreaming}
     */
    public NatsStreaming tryStart() {
        return tryStart(SECONDS.toMillis(10));
    }

    /**
     * Starts the server in {@link ProcessBuilder} with the given parameterConfig {@link NatsStreaming#config(String...)}
     * Throws all exceptions as {@link RuntimeException}
     *
     * @param timeoutMs defines the start up timeout {@code -1} no timeout, else waits until port up
     * @return {@link NatsStreaming}
     */
    public NatsStreaming tryStart(final long timeoutMs) {
        try {
            start(timeoutMs);
            return this;
        } catch (IOException e) {
            throw new NatsStartException(e);
        }
    }

    /**
     * Starts the server in {@link ProcessBuilder} with the given parameterConfig {@link NatsStreaming#config(String...)}
     *
     * @return {@link NatsStreaming}
     * @throws IOException              if {@link NatsStreaming} is not found or unsupported on the {@link SystemUtil}
     * @throws BindException            if port is already taken
     * @throws PortUnreachableException if {@link NatsStreaming} is not starting cause port is not free
     */
    public NatsStreaming start() throws IOException {
        return start(SECONDS.toMillis(10));
    }

    /**
     * Starts the server in {@link ProcessBuilder} with the given parameterConfig {@link NatsStreaming#config(String...)}
     *
     * @param timeoutMs defines the start up timeout {@code -1} no timeout, else waits until port up
     * @return {@link NatsStreaming}
     * @throws IOException              if {@link NatsStreaming} is not found or unsupported on the {@link SystemUtil}
     * @throws BindException            if port is already taken
     * @throws PortUnreachableException if {@link NatsStreaming} is not starting cause port is not free
     */
    public NatsStreaming start(final long timeoutMs) throws IOException {
        if (process != null) {
            LOG.error("[{}] is already running", name);
            return this;
        }

        if (!waitForPort(port(), timeoutMs, true)) {
            throw new BindException("Address already in use [" + port() + "]");
        }

        Path natsServerPath = getNatsServerPath(OS, OS_ARCH, OS_ARCH_TYPE);
        SystemUtil.setFilePermissions(natsServerPath, OWNER_EXECUTE, OTHERS_EXECUTE, OWNER_READ, OTHERS_READ, OWNER_WRITE, OTHERS_WRITE);
        LOG.debug("Starting [{}] port [{}] version [{}]", name, port(), OS);

        String command = prepareCommand(natsServerPath);

        LOG.debug(command);

        final Terminal terminal = new Terminal()
                .consumerInfo(LOG::info)
                .consumerError(LOG::error)
                .timeoutMs(timeoutMs > 0 ? timeoutMs : 10000)
                .breakOnError(false)
                .execute(command);
        process = terminal.process();

        if (!waitForPort(port(), timeoutMs, false)) {
            throw new PortUnreachableException(name + " failed to start with port [" + port() + "]"
                    + "\n" + terminal.consoleInfo()
                    + "\n" + terminal.consoleError());
        }
        LOG.info("Started [{}] port [{}] version [{}] pid [{}]", name, port(), OS, readPid());
        return this;
    }

    /**
     * Stops the {@link ProcessBuilder} and kills the {@link NatsStreaming}
     * Only a log error will occur if the {@link NatsStreaming} were never started
     *
     * @return {@link NatsStreaming}
     */
    public NatsStreaming stop() {
        return stop(-1);
    }

    /**
     * Stops the {@link ProcessBuilder} and kills the {@link NatsStreaming}
     * Only a log error will occur if the {@link NatsStreaming} were never started
     *
     * @param timeoutMs defines the tear down timeout, {@code -1} no timeout, else waits until port is free again
     * @return {@link NatsStreaming}
     */
    public NatsStreaming stop(final long timeoutMs) {
        try {
            LOG.info("Stopping [{}]", name);
            if (pid > 0) {
                new Terminal()
                        .consumerInfo(LOG::info)
                        .consumerError(LOG::error)
                        .breakOnError(false)
                        .execute(getNatsServerPath(OS, OS_ARCH, OS_ARCH_TYPE).toString() + " " + SIGNAL.getKey() + " stop=" + pid);
            }
            process.destroy();
            process.waitFor();
        } catch (NullPointerException | InterruptedException ignored) {
            LOG.warn("Could not find process to stop [{}]", name);
        } finally {
            waitForPort(port(), timeoutMs, true);
            LOG.info("Stopped [{}]", name);
        }
        return tryDeleteFile(pidFile());
    }

    /**
     * Gets the port out of the configuration
     *
     * @return configured port of the server
     * @throws RuntimeException with {@link ConnectException} when there is no port configured
     */
    public int port() {
        String port = config.get(PORT);
        if (port != null) {
            return Integer.parseInt(port);
        }
        throw new MissingFormatArgumentException("Could not initialise port " + name);
    }

    /**
     * Sets the port out of the configuration
     *
     * @param port {@code -1} for random port
     * @return {@link NatsStreaming}
     * @throws RuntimeException with {@link ConnectException} when there is no port configured
     */
    public NatsStreaming port(int port) {
        config(PORT, String.valueOf(port < 1 ? getNextFreePort() : port));
        return this;
    }

    /**
     * Url to find nats server source
     *
     * @param natsServerUrl url of the source {@link NatsStreamingSourceConfig}
     * @return {@link NatsStreaming}
     */
    public NatsStreaming source(final String natsServerUrl) {
        this.source = natsServerUrl;
        return this;
    }

    /**
     * Url to find nats server source
     */
    public String source() {
        return source;
    }

    /**
     * get process id
     *
     * @return process id from nats server
     */
    public int pid() {
        return pid;
    }

    public Path pidFile() {
        return Paths.get(config.computeIfAbsent(
                PID,
                value -> Paths.get(TMP_DIR, name.toLowerCase(), port() + ".pid").toString())
        );
    }

    /**
     * Gets Nats server path
     *
     * @return Resource/{SIMPLE_CLASS_NAME}/{NATS_SERVER_VERSION}/{OPERATING_SYSTEM}/{SIMPLE_CLASS_NAME}
     */
    public Path natsPath() {
        return getNatsServerPath(OS, OS_ARCH, OS_ARCH_TYPE);
    }

    /**
     * Gets Nats server path
     *
     * @return Resource/{SIMPLE_CLASS_NAME}/{NATS_SERVER_VERSION}/{OS}_{OS_ARCH}_{OS_ARCH_TYPE}/{SIMPLE_CLASS_NAME}
     */
    protected Path getDefaultPath() {
        return getNatsServerPath(OS, OS_ARCH, OS_ARCH_TYPE);
    }

    /**
     * Gets Nats server path
     *
     * @return Resource/{SIMPLE_CLASS_NAME}/{NATS_SERVER_VERSION}/{OPERATING_SYSTEM}/{SIMPLE_CLASS_NAME}
     */
    protected Path getNatsServerPath(final OsType os, final OsArch arch, final OsArchType archType) {
        final String targetPath =
                name + File.separator
                        + name + "_" + os + "_" + arch + "_" + archType
                        + (os == OS_WINDOWS ? ".exe" : "");
        return downloadNats(targetPath.toLowerCase()
                .replace("os_", "").replace("_at_", "").replace("_arch", ""));
    }

    private boolean isEmpty(String property) {
        return property == null || property.trim().length() <= 0;
    }

    private NatsStreaming tryDeleteFile(final Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
        return this;
    }

    private int readPid() {
        try {
            pid = Integer.parseInt(String.join(" ", Files.readAllLines(pidFile(), StandardCharsets.UTF_8)).trim());
        } catch (IOException e) {
            throw new NatsFileReaderException("Unable to read PID file [" + pidFile() + "]", e);
        }
        return pid;
    }

    private Path downloadNats(final String targetPath) {
        final Path tmpPath = Paths.get(TMP_DIR, targetPath);
        if (Files.notExists(tmpPath)) {
            final File zipFile = new File(tmpPath.getParent().toFile(), tmpPath.getFileName().toString() + ".zip");
            createParents(tmpPath);
            LOG.info("Start download natsServer from [{}] to [{}]", source, zipFile);
            try (FileOutputStream fos = new FileOutputStream(zipFile)) {
                fos.getChannel().transferFrom(newChannel(new URL(source).openStream()), 0, Long.MAX_VALUE);
                LOG.info("Finished download natsServer unpacked to [{}]", tmpPath.toUri());
                return setExecutable(unzip(zipFile, tmpPath.toFile()));
            } catch (Exception e) {
                throw new NatsStreamingDownloadException(e);
            }
        }
        return tmpPath;
    }

    private NatsStreaming createParents(final Path tmpPath) {
        try {
            Files.createDirectories(tmpPath.getParent());
        } catch (IOException ignored) {
        }
        return this;
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "java:S899"})
    private Path setExecutable(final Path path) {
        path.toFile().setExecutable(true);
        return path;
    }

    private Path unzip(final File source, final File target) throws IOException {
        try (final ZipFile zipFile = new ZipFile(source)) {
            ZipEntry max = zipFile.stream().max(comparingLong(ZipEntry::getSize))
                    .orElseThrow(() -> new IllegalStateException("File not found [" + zipFile + "]"));
            Files.copy(zipFile.getInputStream(max), target.toPath());
            tryDeleteFile(source.toPath());
            return target.toPath();
        }
    }

    public static boolean waitForPort(final int port, final long timeoutMs, boolean isFree) {
        final long start = System.currentTimeMillis();

        while (System.currentTimeMillis() - start < timeoutMs) {
            if (isPortAvailable(port) == isFree) {
                return true;
            }
            Thread.yield();
        }
        return timeoutMs <= 0;
    }

    private static boolean isPortAvailable(final int port) {
        try {
            new Socket("localhost", port).close();
            return false;
        } catch (IOException e) {
            return true;
        }
    }

    private String prepareCommand(final Path natsServerPath) {
        StringBuilder command = new StringBuilder();
        command.append(natsServerPath.toString());
        pidFile();
        for (Entry<NatsStreamingConfig, String> entry : config().entrySet()) {
            String key = entry.getKey().getKey();

            if (isEmpty(entry.getValue())) {
                LOG.warn("Skipping property [{}] with value [{}]", key, entry.getValue());
                continue;
            }

            command.append(" ");

            command.append(key);
            if (!entry.getKey().getDescription().startsWith("[/]")) {
                command.append(entry.getValue().trim().toLowerCase());
            }
        }
        return command.toString();
    }

    private Map<NatsStreamingConfig, String> getDefaultConfig() {
        final Map<NatsStreamingConfig, String> defaultConfig = new EnumMap<>(NatsStreamingConfig.class);
        for (NatsStreamingConfig natsConfig : NatsStreamingConfig.values()) {
            if (natsConfig.getDefaultValue() != null) {
                defaultConfig.put(natsConfig, natsConfig.getDefaultValue().toString());
            }
        }
        return defaultConfig;
    }

    private int getNextFreePort() {
        for (int i = 1; i < 277; i++) {
            final int port = i + (int) PORT.getDefaultValue();
            if (!isPortInUse(port)) {
                return port;
            }
        }
        throw new IllegalStateException("Could not find any free port");
    }

    private boolean isPortInUse(final int portNumber) {
        try {
            new Socket("localhost", portNumber).close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return name + "{" +
                "NATS_SERVER_VERSION=" + source +
                ", OPERATING_SYSTEM=" + OS +
                ", port=" + port() +
                '}';
    }
}