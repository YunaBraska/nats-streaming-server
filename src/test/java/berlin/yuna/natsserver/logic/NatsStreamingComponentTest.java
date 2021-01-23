package berlin.yuna.natsserver.logic;

import berlin.yuna.natsserver.config.NatsStreamingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.BindException;
import java.net.ConnectException;
import java.net.PortUnreachableException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingFormatArgumentException;

import static berlin.yuna.clu.logic.SystemUtil.OperatingSystem.LINUX;
import static berlin.yuna.clu.logic.SystemUtil.OperatingSystem.WINDOWS;
import static berlin.yuna.clu.logic.SystemUtil.getOsType;
import static berlin.yuna.natsserver.config.NatsStreamingConfig.AUTH;
import static berlin.yuna.natsserver.config.NatsStreamingConfig.HB_FAIL_COUNT;
import static berlin.yuna.natsserver.config.NatsStreamingConfig.MAX_AGE;
import static berlin.yuna.natsserver.config.NatsStreamingConfig.PASS;
import static berlin.yuna.natsserver.config.NatsStreamingConfig.PORT;
import static berlin.yuna.natsserver.config.NatsStreamingConfig.TRACE;
import static berlin.yuna.natsserver.config.NatsStreamingConfig.USER;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("UnitTest")
@DisplayName("NatsServer plain java")
class NatsStreamingComponentTest {

    private String natsSource;
    private static final String USER_DIR = System.getProperty("user.dir");

    @BeforeEach
    void setUp() {
        natsSource = getOsType().equals(LINUX) ?
                "file://" + USER_DIR + "/src/test/resources/natsserver/linux.zip" :
                "file://" + USER_DIR + "/src/test/resources/natsserver/mac.zip";
    }

    @Test
    @DisplayName("No start without annotation")
    void natsServer_withoutAnnotation_shouldNotBeStarted() {
        assertThrows(
                ConnectException.class,
                () -> new Socket("localhost", 4245).close(),
                "Connection refused"
        );
    }

    @Test
    @DisplayName("Default config")
    void natsServer_withoutConfig_shouldStartWithDefaultValues() {
        NatsStreaming nats = new NatsStreaming().config(HB_FAIL_COUNT, "5").port(4238).source(natsSource);
        assertThat(nats.source(), is(equalTo(natsSource)));
        nats.tryStart(SECONDS.toMillis(10));
        nats.stop();
        assertThat(nats.toString().length(), is(greaterThan(1)));
    }

    @Test
    @DisplayName("Setup config")
    void natsServer_configureConfig_shouldNotOverwriteOldConfig() {
        NatsStreaming nats = new NatsStreaming(4240).source(natsSource);
        nats.setConfig("user:adminUser", "PAss:adminPw");

        assertThat(nats.getConfig().get(USER), is(equalTo("adminUser")));
        assertThat(nats.getConfig().get(PASS), is(equalTo("adminPw")));

        nats.setConfig("user:newUser");
        assertThat(nats.getConfig().get(USER), is(equalTo("newUser")));
        assertThat(nats.getConfig().get(PASS), is("adminPw"));

        Map<NatsStreamingConfig, String> newConfig = new HashMap<>();
        newConfig.put(USER, "oldUser");
        nats.setConfig(newConfig);
        assertThat(nats.getConfig().get(USER), is(equalTo("oldUser")));
    }

    @Test
    @DisplayName("Unknown config is ignored")
    void natsServer_invalidConfig_shouldNotRunIntroException() {
        NatsStreaming nats = new NatsStreaming(4240).source(natsSource);
        nats.setConfig("user:adminUser:password", " ", "auth:isValid", "");
        assertThat(nats.getConfig().size(), is(23));
        assertThat(nats.getConfig().get(AUTH), is(equalTo("isValid")));
    }

    @Test
    @DisplayName("Duplicate starts will be ignored")
    void natsServer_duplicateStart_shouldNotRunIntroExceptionOrInterrupt() throws IOException {
        NatsStreaming nats = new NatsStreaming(4231).source(natsSource);
        nats.start();
        nats.start(SECONDS.toMillis(10));
        nats.stop(SECONDS.toMillis(10));
    }

    @Test
    @DisplayName("Unknown config [FAIL]")
    void natsServer_withWrongConfig_shouldNotStartAndThrowException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new NatsStreaming("unknown:config", "port:4232").source(natsSource),
                "No enum constant"
        );
    }

    @Test
    @DisplayName("Duplicate instances [FAIL]")
    void natsServer_asTwoInstances_shouldThrowBindException() {
        NatsStreaming nats_Streaming_Java_one = new NatsStreaming(4233).source(natsSource);
        NatsStreaming nats_Streaming_Java_two = new NatsStreaming(4233).source(natsSource);
        Exception exception = null;
        try {
            nats_Streaming_Java_one.start();
            nats_Streaming_Java_two.start();
        } catch (Exception e) {
            exception = e;
        } finally {
            nats_Streaming_Java_one.stop();
            nats_Streaming_Java_two.stop();
        }
        assertThat(requireNonNull(exception).getClass().getSimpleName(), is(equalTo(BindException.class.getSimpleName())));
    }

    @Test
    @DisplayName("Stop without start will be ignored")
    void natsServer_stopWithoutStart_shouldNotRunIntroExceptionOrInterrupt() {
        NatsStreaming nats = new NatsStreaming(4241).source(natsSource);
        nats.stop();
    }

    @Test
    @DisplayName("Config port with NULL [FAIL]")
    void natsServer_withNullablePortValue_shouldThrowMissingFormatArgumentException() {
        NatsStreaming nats = new NatsStreaming(4243).source(natsSource);
        nats.getConfig().put(PORT, null);
        assertThrows(
                MissingFormatArgumentException.class,
                nats::port,
                "Could not initialise port"
        );
    }

    @Test
    @DisplayName("Configure with NULL value should be ignored")
    void natsServer_withNullableConfigValue_shouldNotRunIntroExceptionOrInterrupt() throws IOException {
        NatsStreaming nats = new NatsStreaming(4236).source(natsSource);
        nats.getConfig().put(MAX_AGE, null);
        nats.start();
        nats.stop();
    }

    @Test
    @DisplayName("Configure with invalid config value [FAIL]")
    void natsServer_withInvalidConfigValue_shouldNotRunIntroExceptionOrInterrupt() {
        NatsStreaming nats = new NatsStreaming(MAX_AGE + ":invalidValue", PORT + ":4237").source(natsSource);
        assertThrows(
                PortUnreachableException.class,
                nats::start,
                "NatsServer failed to start"
        );
        nats.stop();
    }

    @Test
    @DisplayName("Validate Windows path")
    void natsServerOnWindows_shouldAddExeToPath() {
        NatsStreaming nats = new NatsStreaming(4244).source(natsSource);
        String windowsNatsServerPath = nats.getNatsServerPath(WINDOWS).toString();
        String expectedExe = nats.name.toLowerCase() + ".exe";
        assertThat(windowsNatsServerPath, containsString(expectedExe));
    }

    @Test
    @DisplayName("Config without url [FAIL]")
    @SuppressWarnings("ResultOfMethodCallIgnored")
    void natsServerWithoutSourceUrl_shouldThrowException() {
        NatsStreaming nats = new NatsStreaming(4239).source(natsSource);
        nats.getNatsServerPath(getOsType()).toFile().delete();
        nats.source(null);
        assertThrows(
                RuntimeException.class,
                nats::start,
                "Could not initialise port"
        );
        assertThrows(
                RuntimeException.class,
                nats::tryStart,
                "Could not initialise port"
        );
    }

    @Test
    @DisplayName("Configure without value param")
    void natsServer_withoutValue() throws IOException {
        NatsStreaming nats = new NatsStreaming(4242).source(natsSource);
        nats.getConfig().put(TRACE, "true");
        nats.start();
        nats.stop();
    }

    @Test
    @DisplayName("Nats server with random port")
    void natsServer_withRandomPort() {
        NatsStreaming nats = new NatsStreaming(-1).source(natsSource);
        assertThat(nats.port(), is(not((int) PORT.getDefaultValue())));
        assertThat(nats.port(), is(greaterThan((int) PORT.getDefaultValue())));
        assertThat(nats.port(), is(lessThan((int) PORT.getDefaultValue() + 501)));
    }
}
