package berlin.yuna.natsserver.logic;

import berlin.yuna.clu.logic.SystemUtil;
import berlin.yuna.clu.logic.SystemUtil.OperatingSystem;
import berlin.yuna.clu.logic.Terminal;
import berlin.yuna.natsserver.config.NatsServerConfig;
import berlin.yuna.natsserver.config.NatsServerSourceConfig;
import berlin.yuna.natsserver.model.exception.NatsDownloadException;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.BindException;
import java.net.ConnectException;
import java.net.PortUnreachableException;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.MissingFormatArgumentException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static berlin.yuna.clu.logic.SystemUtil.OperatingSystem.WINDOWS;
import static berlin.yuna.clu.logic.SystemUtil.getOsType;
import static berlin.yuna.clu.logic.SystemUtil.killProcessByName;
import static berlin.yuna.natsserver.config.NatsServerConfig.PORT;
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
 * {@link Nats}
 *
 * @author Yuna Morgenstern
 * @see OperatingSystem
 * @see Nats
 * @since 1.0
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class Nats {

    /**
     * simpleName from {@link Nats} class
     */
    protected final String name;
    protected static final Logger LOG = getLogger(Nats.class);
    protected static final OperatingSystem OPERATING_SYSTEM = getOsType();
    protected static final String TMP_DIR = System.getProperty("java.io.tmpdir");

    private Process process;
    private String source = NatsServerSourceConfig.valueOf(getOsType().toString().replace("UNKNOWN", "DEFAULT")).getDefaultValue();
    private Map<NatsServerConfig, String> natsServerConfig = getDefaultConfig();

    /**
     * Create {@link Nats} without any start able configuration
     */
    public Nats() {
         name = Nats.class.getSimpleName();
    }

    /**
     * Create {@link Nats} with simplest start able configuration
     *
     * @param port start port - common default port is 4222
     */
    public Nats(int port) {
        this();
        natsServerConfig.put(PORT, String.valueOf(port));
    }

    /**
     * Create custom {@link Nats} with simplest configuration {@link Nats#setNatsServerConfig(String...)}
     *
     * @param natsServerConfig passes the original parameters to the server. example: port:4222, user:admin, password:admin
     */
    public Nats(final String... natsServerConfig) {
        this();
        this.setNatsServerConfig(natsServerConfig);
    }

    /**
     * GetNatServerConfig
     *
     * @return the {@link Nats} configuration but not the config of the real PID
     */
    public Map<NatsServerConfig, String> getNatsServerConfig() {
        return natsServerConfig;
    }

    /**
     * Passes the original parameters to the server on startup
     *
     * @param natsServerConfig passes the original parameters to the server.
     * @return {@link Nats}
     * @see Nats#setNatsServerConfig(String...)
     * @see NatsServerConfig
     */
    public Nats setNatsServerConfig(final Map<NatsServerConfig, String> natsServerConfig) {
        this.natsServerConfig = natsServerConfig;
        return this;
    }

    /**
     * Passes the original parameters to the server on startup
     *
     * @param natsServerConfigArray example: port:4222, user:admin, password:admin
     * @return {@link Nats}
     * @see NatsServerConfig
     */
    public Nats setNatsServerConfig(final String... natsServerConfigArray) {
        for (String property : natsServerConfigArray) {
            String[] pair = property.split(":");
            if (isEmpty(property) || pair.length != 2) {
                LOG.error("Could not parse property [{}] pair length [{}]", property, pair.length);
                continue;
            }
            natsServerConfig.put(NatsServerConfig.valueOf(pair[0].toUpperCase().replace("-", "")), pair[1]);
        }
        return this;
    }

    private boolean isEmpty(String property) {
        return property == null || property.trim().length() <= 0;
    }

    /**
     * Starts the server in {@link ProcessBuilder} with the given parameterConfig {@link Nats#setNatsServerConfig(String...)}
     *
     * @return {@link Nats}
     * @throws IOException              if {@link Nats} is not found or unsupported on the {@link OperatingSystem}
     * @throws BindException            if port is already taken
     * @throws PortUnreachableException if {@link Nats} is not starting cause port is not free
     */
    public Nats start() throws IOException {
        if (process != null) {
            LOG.error("[{}] is already running", name);
            return this;
        }

        if (!waitForPort(true)) {
            throw new BindException("Address already in use [" + port() + "]");
        }

        Path natsServerPath = getNatsServerPath(OPERATING_SYSTEM);
        SystemUtil.setFilePermissions(natsServerPath, OWNER_EXECUTE, OTHERS_EXECUTE, OWNER_READ, OTHERS_READ, OWNER_WRITE, OTHERS_WRITE);
        LOG.info("Starting [{}] port [{}] version [{}]", name, port(), OPERATING_SYSTEM);

        String command = prepareCommand(natsServerPath);

        LOG.debug(command);

        final Terminal terminal = new Terminal()
                .consumerInfo(LOG::info)
                .consumerError(LOG::error)
                .timeoutMs(10000)
                .breakOnError(false)
                .execute(command);
        process = terminal.process();

        if (!waitForPort(false)) {
            throw new PortUnreachableException(name + " failed to start with port [" + port() + "]"
                    + "\n" + terminal.consoleInfo()
                    + "\n" + terminal.consoleError());
        }
        LOG.info("Started [{}] port [{}] version [{}]", name, port(), OPERATING_SYSTEM);
        return this;
    }

    /**
     * Stops the {@link ProcessBuilder} and kills the {@link Nats}
     * Only a log error will occur if the {@link Nats} were never started
     *
     * @return {@link Nats}
     * @throws RuntimeException as {@link InterruptedException} if shutdown is interrupted
     */
    public Nats stop() {
        try {
            LOG.info("Stopping [{}]", name);
            process.destroy();
            process.waitFor();
        } catch (NullPointerException | InterruptedException ignored) {
            LOG.warn("Could not stop [{}] cause cant find process", name);
            killProcessByName(getNatsServerPath(OPERATING_SYSTEM).getFileName().toString());
        } finally {
            LOG.info("Stopped [{}]", name);
        }
        return this;
    }

    /**
     * Gets the port out of the configuration not from the real PID
     *
     * @return configured port of the server
     * @throws RuntimeException with {@link ConnectException} when there is no port configured
     */
    public int port() {
        String port = natsServerConfig.get(PORT);
        if (port != null) {
            return Integer.parseInt(port);
        }
        throw new MissingFormatArgumentException("Could not initialise port " + name);
    }

    /**
     * Sets the port out of the configuration not from the real PID
     *
     * @return {@link Nats}
     * @throws RuntimeException with {@link ConnectException} when there is no port configured
     */
    public Nats port(int port) {
        natsServerConfig.put(PORT, String.valueOf(port));
        return this;
    }

    /**
     * Url to find nats server source
     *
     * @param natsServerUrl url of the source {@link NatsServerSourceConfig}
     * @return {@link Nats}
     */
    public Nats source(final String natsServerUrl) {
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
     * Gets Nats server path
     *
     * @return Resource/{SIMPLE_CLASS_NAME}/{NATS_SERVER_VERSION}/{OPERATING_SYSTEM}/{SIMPLE_CLASS_NAME}
     */
    protected Path getNatsServerPath(final OperatingSystem operatingSystem) {
        final String targetPath = name.toLowerCase() + File.separator +
                operatingSystem + File.separator +
                name.toLowerCase() + (operatingSystem == WINDOWS ? ".exe" : "");
        return downloadNats(targetPath);
    }

    private Path downloadNats(final String targetPath) {
        final Path tmpPath = Paths.get(TMP_DIR, targetPath);
        if (Files.notExists(tmpPath)) {
            final File zipFile = new File(tmpPath.getParent().toFile(), tmpPath.getFileName().toString() + ".zip");
            LOG.info("Start download natsServer from [{}] to [{}]", source, zipFile);
            createParents(tmpPath);
            try (FileOutputStream fos = new FileOutputStream(zipFile)) {
                fos.getChannel().transferFrom(newChannel(new URL(source).openStream()), 0, Long.MAX_VALUE);
                return setExecutable(unzip(zipFile, tmpPath.toFile()));
            } catch (Exception e) {
                throw new NatsDownloadException(e);
            }
        }
        LOG.info("Finished download natsServer unpacked to [{}]", tmpPath.toUri());
        return tmpPath;
    }

    private void createParents(final Path tmpPath) {
        try {
            Files.createDirectories(tmpPath.getParent());
        } catch (IOException ignored) {
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private Path setExecutable(final Path path) {
        path.toFile().setExecutable(true);
        return path;
    }

    private Path unzip(final File source, final File target) throws IOException {
        try (final ZipFile zipFile = new ZipFile(source)) {
            ZipEntry max = zipFile.stream().max(comparingLong(ZipEntry::getSize))
                    .orElseThrow(() -> new IllegalStateException("File not found " + zipFile));
            Files.copy(zipFile.getInputStream(max), target.toPath());
            Files.delete(source.toPath());
            return target.toPath();
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean waitForPort(boolean isFree) {
        final long start = System.currentTimeMillis();
        long timeout = SECONDS.toMillis(10);

        while (System.currentTimeMillis() - start < timeout) {
            if (isPortAvailable(port()) == isFree) {
                return true;
            }
            Thread.yield();
        }
        return false;
    }

    private static boolean isPortAvailable(int port) {
        try {
            new Socket("localhost", port).close();
            return false;
        } catch (IOException e) {
            return true;
        }
    }

    private String prepareCommand(Path natsServerPath) {
        StringBuilder command = new StringBuilder();
        command.append(natsServerPath.toString());
        for (Entry<NatsServerConfig, String> entry : getNatsServerConfig().entrySet()) {
            String key = entry.getKey().getKey();

            if (isEmpty(entry.getValue())) {
                LOG.warn("Skipping property [{}] with value [{}]", key, entry.getValue());
                continue;
            }

            command.append(" ");

            command.append(key);
            command.append(entry.getValue().trim().toLowerCase());
        }
        return command.toString();
    }

    private Map<NatsServerConfig, String> getDefaultConfig() {
        final Map<NatsServerConfig, String> defaultConfig = new EnumMap<>(NatsServerConfig.class);
        for (NatsServerConfig natsConfig : NatsServerConfig.values()) {
            if (natsConfig.getDefaultValue() != null) {
                defaultConfig.put(natsConfig, natsConfig.getDefaultValue().toString());
            }
        }
        return defaultConfig;
    }

    @Override
    public String toString() {
        return name + "{" +
                "NATS_SERVER_VERSION=" + source +
                ", OPERATING_SYSTEM=" + OPERATING_SYSTEM +
                ", port=" + port() +
                '}';
    }
}