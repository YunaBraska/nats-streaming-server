package berlin.yuna.natsserver.config;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static berlin.yuna.natsserver.config.NatsStreamingConfig.ARGS_SEPARATOR;
import static berlin.yuna.natsserver.config.NatsStreamingOptionsBuilder.getValue;
import static berlin.yuna.natsserver.config.NatsStreamingOptionsBuilder.getValueB;
import static berlin.yuna.natsserver.config.NatsStreamingOptionsBuilder.getValueI;

@SuppressWarnings("java:S2176")
public class NatsStreamingOptions implements io.nats.commons.NatsOptions {

    protected final Logger logger;
    protected final Map<NatsStreamingConfig, String> config;

    public NatsStreamingOptions(final Logger logger, final Map<NatsStreamingConfig, String> config) {
        this.logger = logger;
        this.config = config == null ? new EnumMap<>(NatsStreamingConfig.class) : new EnumMap<>(config);
    }

    /**
     * @return Nats version
     * @see NatsStreamingConfig#NATS_STREAMING_VERSION
     */
    public String version() {
        return config.get(NatsStreamingConfig.NATS_STREAMING_VERSION);
    }

    /**
     * @return The port to start on or &lt;=0 to use an automatically allocated port
     * @see NatsStreamingConfig#PORT
     */
    @Override
    public Integer port() {
        return getValueI(config, NatsStreamingConfig.PORT);
    }

    /**
     * @return always false as this is a feature of Nats not Nats Streaming server
     */
    @Override
    public Boolean jetStream() {
        return false;
    }

    /**
     * @return true if debug is enabled
     * @see NatsStreamingConfig#DEBUG
     */
    @Override
    public Boolean debug() {
        return getValueB(config, NatsStreamingConfig.DEBUG);
    }

    /**
     * @return path to a custom config file
     * @see NatsStreamingConfig#CONFIG
     */
    @Override
    public Path configFile() {
        return getValue(config, Path::of, NatsStreamingConfig.CONFIG);
    }

    /**
     * @return custom args to add to the command line
     * @see NatsStreamingConfig#NATS_ARGS
     */
    @Override
    public String[] customArgs() {
        return getValue(config, args -> args.split(ARGS_SEPARATOR), NatsStreamingConfig.NATS_ARGS);
    }

    /**
     * @return custom logger
     */
    @Override
    public Logger logger() {
        return logger;
    }

    /**
     * @return custom LogLevel
     */
    @Override
    public Level logLevel() {
        return getValue(config, NatsStreamingConfig::logLevelOf, NatsStreamingConfig.NATS_LOG_LEVEL);
    }

    /**
     * @return configMap
     * @see NatsStreamingConfig
     */
    public Map<NatsStreamingConfig, String> config() {
        return config;
    }

    public static NatsStreamingOptionsBuilder natsStreamingBuilder() {
        return new NatsStreamingOptionsBuilder();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final NatsStreamingOptions that = (NatsStreamingOptions) o;
        return Objects.equals(logger, that.logger) && Objects.equals(config, that.config);
    }

    @Override
    public int hashCode() {
        return Objects.hash(logger, config);
    }
}
