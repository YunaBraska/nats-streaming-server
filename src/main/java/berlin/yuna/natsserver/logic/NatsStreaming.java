package berlin.yuna.natsserver.logic;

import berlin.yuna.clu.logic.SystemUtil;
import berlin.yuna.clu.logic.Terminal;
import berlin.yuna.natsserver.config.NatsStreamingConfig;
import berlin.yuna.natsserver.model.MapValue;
import berlin.yuna.natsserver.model.NatsStartException;

import java.io.IOException;
import java.net.BindException;
import java.net.PortUnreachableException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static berlin.yuna.natsserver.config.NatsStreamingConfig.NATS_SYSTEM;
import static berlin.yuna.natsserver.config.NatsStreamingConfig.PORT;
import static berlin.yuna.natsserver.config.NatsStreamingConfig.SIGNAL;
import static berlin.yuna.natsserver.logic.NatsUtils.validatePort;
import static berlin.yuna.natsserver.logic.NatsUtils.waitForPort;
import static berlin.yuna.natsserver.model.ValueSource.DSL;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * {@link NatsStreaming}
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class NatsStreaming extends NatsBase {

    /**
     * Create {@link NatsStreaming} without any start able configuration
     */
    public NatsStreaming() {
        super(new ArrayList<>());
    }

    /**
     * Create {@link NatsStreaming} with custom args
     */
    public NatsStreaming(final List<String> customArgs) {
        super(customArgs);
    }

    /**
     * Create {@link NatsStreaming} with the simplest start able configuration
     *
     * @param port start port, -1 = random, default is 4222
     */
    public NatsStreaming(final int port) {
        this();
        config(PORT, String.valueOf(port));
    }

    /**
     * Create custom {@link NatsStreaming} with the simplest configuration {@link NatsStreaming#config(String...)}
     *
     * @param kv passes the original parameters to the server. example: port:4222, user:admin, password:admin
     */
    public NatsStreaming(final String... kv) {
        this();
        this.config(kv);
    }

    /**
     * GetNatServerConfig
     *
     * @return the {@link NatsStreaming} configuration
     */
    public Map<NatsStreamingConfig, MapValue> config() {
        return config;
    }

    /**
     * Configures the nats server
     *
     * @return the {@link NatsStreaming} configuration
     */
    public NatsStreaming config(final NatsStreamingConfig key, final String value) {
        config.remove(key);
        if (key.desc().startsWith("[/]")) {
            if (value.equals("true")) {
                addConfig(DSL, key, value);
            }
        } else {
            addConfig(DSL, key, value);
        }
        return this;
    }

    /**
     * Configures the nats server
     *
     * @param config passes the original parameters to the server.
     * @return {@link NatsStreaming}
     * @see NatsStreaming#config(String...)
     * @see NatsStreamingConfig
     */
    public NatsStreaming config(final Map<NatsStreamingConfig, String> config) {
        config.forEach((key, value) -> addConfig(DSL, key, value));
        return this;
    }

    /**
     * Configures the nats server
     *
     * @param kv example: port, 4222, user, admin, password, admin
     * @return {@link NatsStreaming}
     * @see NatsStreamingConfig
     */
    public NatsStreaming config(final String... kv) {
        boolean isKey = true;
        String key = null;
        for (String property : kv) {
            if (isKey) {
                key = property;
            } else {
                config(NatsStreamingConfig.valueOf(key.toUpperCase().replace("-", "")), property);
            }
            isKey = !isKey;
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
     * Starts the server in {@link ProcessBuilder} with the given config {@link NatsStreaming#config(String...)}
     * Throws all exceptions as {@link RuntimeException}
     *
     * @param timeoutMs defines the start-up timeout {@code -1} no timeout, else waits until port up
     * @return {@link NatsStreaming}
     */
    public NatsStreaming tryStart(final long timeoutMs) {
        try {
            start(timeoutMs);
            return this;
        } catch (Exception e) {
            throw new NatsStartException(e);
        }
    }

    /**
     * Starts the server in {@link ProcessBuilder} with the given config {@link NatsStreaming#config(String...)}
     *
     * @return {@link NatsStreaming}
     * @throws IOException              if {@link NatsStreaming} is not found or unsupported on the {@link SystemUtil}
     * @throws BindException            if port is already taken
     * @throws PortUnreachableException if {@link NatsStreaming} is not starting cause port is not free
     */
    public NatsStreaming start() throws Exception {
        return start(SECONDS.toMillis(10));
    }

    /**
     * Starts the server in {@link ProcessBuilder} with the given config {@link NatsStreaming#config(String...)}
     *
     * @param timeoutMs defines the start-up timeout {@code -1} no timeout, else waits until port up
     * @return {@link NatsStreaming}
     * @throws IOException              if {@link NatsStreaming} is not found or unsupported on the {@link SystemUtil}
     * @throws BindException            if port is already taken
     * @throws PortUnreachableException if {@link NatsStreaming} is not starting cause port is not free
     */
    @SuppressWarnings({"java:S899"})
    public synchronized NatsStreaming start(final long timeoutMs) throws Exception {
        if (process != null) {
            logger.severe(() -> format("[%s] is already running", name));
            return this;
        }

        setNextFreePort();
        final int port = port();
        validatePort(port, timeoutMs, true, () -> new BindException("Address already in use [" + port + "]"));

        final Path binaryPath = downloadNats();
        logger.fine(() -> format("Starting [%s] port [%s] version [%s]", name, port, getValue(NATS_SYSTEM)));

        final Terminal terminal = new Terminal()
                .consumerInfo(logger::info)
                .consumerError(logger::severe)
                .timeoutMs(timeoutMs > 0 ? timeoutMs : 10000)
                .breakOnError(false)
                .execute(prepareCommand());
        process = terminal.process();

        validatePort(port, timeoutMs, false, () -> new PortUnreachableException(name + " failed to start with port [" + port + "]"));
        logger.info(() -> format("Started [%s] port [%s] version [%s] pid [%s]", name, port, getValue(NATS_SYSTEM), pid()));
        return this;
    }

    @Override
    public void close() {
        this.stop();
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
    public synchronized NatsStreaming stop(final long timeoutMs) {
        try {
            logger.info(() -> format("Stopping [%s]", name));
            if (pid() != -1) {
                new Terminal()
                        .consumerInfo(logger::info)
                        .consumerError(logger::severe)
                        .breakOnError(false)
                        .execute(binaryFile() + " " + SIGNAL.key() + " stop=" + pid());
            }
            process.destroy();
            process.waitFor();
        } catch (NullPointerException | InterruptedException ignored) {
            logger.warning(() -> format("Could not find process to stop [%s]", name));
            Thread.currentThread().interrupt();
        } finally {
            if (port() > -1) {
                waitForPort(port(), timeoutMs, true);
                logger.info(() -> format("Stopped [%s]", name));
            }
        }
        deletePidFile();
        return this;
    }
}