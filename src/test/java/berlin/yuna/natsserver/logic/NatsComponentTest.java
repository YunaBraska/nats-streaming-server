package berlin.yuna.natsserver.logic;

import berlin.yuna.natsserver.config.NatsServerConfig;
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
import static berlin.yuna.natsserver.config.NatsServerConfig.AUTH;
import static berlin.yuna.natsserver.config.NatsServerConfig.HB_FAIL_COUNT;
import static berlin.yuna.natsserver.config.NatsServerConfig.MAX_AGE;
import static berlin.yuna.natsserver.config.NatsServerConfig.PASS;
import static berlin.yuna.natsserver.config.NatsServerConfig.PORT;
import static berlin.yuna.natsserver.config.NatsServerConfig.USER;
import static java.util.Objects.requireNonNull;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("UnitTest")
@DisplayName("NatsServer plain java")
class NatsComponentTest {

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
        Nats nats = new Nats().config(HB_FAIL_COUNT, "5").port(4238).source(natsSource);
        assertThat(nats.source(), is(equalTo(natsSource)));
        nats.tryStart();
        nats.stop();
        assertThat(nats.toString().length(), is(greaterThan(1)));
    }

    @Test
    @DisplayName("Setup config")
    void natsServer_configureConfig_shouldNotOverwriteOldConfig() {
        Nats nats = new Nats(4240).source(natsSource);
        nats.setNatsServerConfig("user:adminUser", "PAss:adminPw");

        assertThat(nats.getNatsServerConfig().get(USER), is(equalTo("adminUser")));
        assertThat(nats.getNatsServerConfig().get(PASS), is(equalTo("adminPw")));

        nats.setNatsServerConfig("user:newUser");
        assertThat(nats.getNatsServerConfig().get(USER), is(equalTo("newUser")));
        assertThat(nats.getNatsServerConfig().get(PASS), is("adminPw"));

        Map<NatsServerConfig, String> newConfig = new HashMap<>();
        newConfig.put(USER, "oldUser");
        nats.setNatsServerConfig(newConfig);
        assertThat(nats.getNatsServerConfig().get(USER), is(equalTo("oldUser")));
    }

    @Test
    @DisplayName("Unknown config is ignored")
    void natsServer_invalidConfig_shouldNotRunIntroException() {
        Nats nats = new Nats(4240).source(natsSource);
        nats.setNatsServerConfig("user:adminUser:password", " ", "auth:isValid", "");
        assertThat(nats.getNatsServerConfig().size(), is(23));
        assertThat(nats.getNatsServerConfig().get(AUTH), is(equalTo("isValid")));
    }

    @Test
    @DisplayName("Duplicate starts will be ignored")
    void natsServer_duplicateStart_shouldNotRunIntroExceptionOrInterrupt() throws IOException {
        Nats nats = new Nats(4231).source(natsSource);
        nats.start();
        nats.start();
        nats.stop();
    }

    @Test
    @DisplayName("Unknown config [FAIL]")
    void natsServer_withWrongConfig_shouldNotStartAndThrowException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Nats("unknown:config", "port:4232").source(natsSource),
                "No enum constant"
        );
    }

    @Test
    @DisplayName("Duplicate instances [FAIL]")
    void natsServer_asTwoInstances_shouldThrowBindException() {
        Nats nats_Java_one = new Nats(4233).source(natsSource);
        Nats nats_Java_two = new Nats(4233).source(natsSource);
        Exception exception = null;
        try {
            nats_Java_one.start();
            nats_Java_two.start();
        } catch (Exception e) {
            exception = e;
        } finally {
            nats_Java_one.stop();
            nats_Java_two.stop();
        }
        assertThat(requireNonNull(exception).getClass().getSimpleName(), is(equalTo(BindException.class.getSimpleName())));
    }

    @Test
    @DisplayName("Stop without start will be ignored")
    void natsServer_stopWithoutStart_shouldNotRunIntroExceptionOrInterrupt() {
        Nats nats = new Nats(4241).source(natsSource);
        nats.stop();
    }

    @Test
    @DisplayName("Config port with NULL [FAIL]")
    void natsServer_withNullablePortValue_shouldThrowMissingFormatArgumentException() {
        Nats nats = new Nats(4243).source(natsSource);
        nats.getNatsServerConfig().put(PORT, null);
        assertThrows(
                MissingFormatArgumentException.class,
                nats::port,
                "Could not initialise port"
        );
    }

    @Test
    @DisplayName("Configure with NULL value should be ignored")
    void natsServer_withNullableConfigValue_shouldNotRunIntroExceptionOrInterrupt() throws IOException {
        Nats nats = new Nats(4236).source(natsSource);
        nats.getNatsServerConfig().put(MAX_AGE, null);
        nats.start();
        nats.stop();
    }

    @Test
    @DisplayName("Configure with invalid config value [FAIL]")
    void natsServer_withInvalidConfigValue_shouldNotRunIntroExceptionOrInterrupt() {
        Nats nats = new Nats(MAX_AGE + ":invalidValue", PORT + ":4237").source(natsSource);
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
        Nats nats = new Nats(4244).source(natsSource);
        String windowsNatsServerPath = nats.getNatsServerPath(WINDOWS).toString();
        String expectedExe = nats.name.toLowerCase() + ".exe";
        assertThat(windowsNatsServerPath, containsString(expectedExe));
    }

    @Test
    @DisplayName("Config without url [FAIL]")
    @SuppressWarnings("ResultOfMethodCallIgnored")
    void natsServerWithoutSourceUrl_shouldThrowException() {
        Nats nats = new Nats(4239).source(natsSource);
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
}
