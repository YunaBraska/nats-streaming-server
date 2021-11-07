package berlin.yuna.natsserver.config;

import berlin.yuna.clu.model.OsArch;
import berlin.yuna.clu.model.OsArchType;
import berlin.yuna.clu.model.OsType;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public enum NatsStreamingSourceConfig {


    URL("https://github.com/nats-io/nats-streaming-server/releases/download/v0.23.0/nats-streaming-server-v0.23.0#SYSTEM#.zip", "[STRING] DEFAULT SOURCE URL");

    private final String defaultValue;
    private final String description;

    NatsStreamingSourceConfig(final String defaultValue, final String description) {
        this.defaultValue = defaultValue;
        this.description = description;
    }

    public String getDefaultValue(final OsType os, OsArch arch, final OsArchType archType) {
        return defaultValue
                .replace("#SYSTEM#", osString(os, "-") + osString(arch, "-") + osString(archType, null))
                .replace("mips64", "linux-mips64le");
    }

    public String getDescription() {
        return description;
    }

    private static String osString(final Enum<?> input, final String prefix) {
        if (input != null && !input.name().contains("UNKNOWN")) {
            return (prefix == null ? "" : "-") + input.toString().toLowerCase()
                    .replace("86", "386")
                    .replace("intel", "")
                    .replace("_", "");
        }
        return "";
    }
}
