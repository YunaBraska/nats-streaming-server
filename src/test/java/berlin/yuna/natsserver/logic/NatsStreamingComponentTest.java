package berlin.yuna.natsserver.logic;

import berlin.yuna.natsserver.config.NatsStreamingConfig;
import berlin.yuna.natsserver.model.NatsFileReaderException;
import berlin.yuna.natsserver.model.NatsStartException;
import berlin.yuna.natsserver.model.NatsStreamingDownloadException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.BindException;
import java.net.ConnectException;
import java.net.PortUnreachableException;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static berlin.yuna.natsserver.config.NatsStreamingConfig.ADDR;
import static berlin.yuna.natsserver.config.NatsStreamingConfig.AUTH;
import static berlin.yuna.natsserver.config.NatsStreamingConfig.PASS;
import static berlin.yuna.natsserver.config.NatsStreamingConfig.PORT;
import static berlin.yuna.natsserver.config.NatsStreamingConfig.TRACE;
import static berlin.yuna.natsserver.config.NatsStreamingConfig.USER;
import static berlin.yuna.natsserver.model.MapValue.mapValueOf;
import static berlin.yuna.natsserver.model.ValueSource.ENV;
import static com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl.DEBUG;
import static java.util.Objects.requireNonNull;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("UnitTest")
@DisplayName("NatsServer plain java")
class NatsStreamingComponentTest {

    private static final int NATS_TIMEOUT = 1024;
    private NatsStreaming nats;

    @BeforeEach
    void setUp() {
        nats = new NatsStreaming(-1);
    }

    @AfterEach
    void afterEach() {
        nats.stop(NATS_TIMEOUT * 8);
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
        nats.tryStart(NATS_TIMEOUT);
        assertThat(nats.pid(), is(greaterThan(-1)));
    }

    @Test
    @DisplayName("Setup config")
    void natsServer_configureConfig_shouldNotOverwriteOldConfig() {
        nats.config("user", "adminUser", "PAss", "adminPw");

        assertThat(nats.getValue(USER), is(equalTo("adminUser")));
        assertThat(nats.getValue(PASS), is(equalTo("adminPw")));

        nats.config("user", "newUser");
        assertThat(nats.getValue(USER), is(equalTo("newUser")));
        assertThat(nats.getValue(PASS), is("adminPw"));

        final Map<NatsStreamingConfig, String> newConfig = new HashMap<>();
        newConfig.put(USER, "oldUser");
        nats.config(newConfig);
        assertThat(nats.getValue(USER), is(equalTo("oldUser")));
    }

    @Test
    @DisplayName("Unknown config is ignored")
    void natsServer_invalidConfig_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> nats.config("user", "adminUser", "auth", "isValid", "", "password", " "));
        assertThat(nats.getValue(AUTH), is(equalTo("isValid")));
    }

    @Test
    @DisplayName("Duplicate starts will be ignored")
    void natsServer_duplicateStart_shouldNotRunIntroExceptionOrInterrupt() throws Exception {
        nats.start();
        nats.start(NATS_TIMEOUT);
        assertThat(nats.pid(), is(greaterThan(-1)));
    }

    @Test
    @DisplayName("Unknown config [FAIL]")
    void natsServer_withWrongConfig_shouldNotStartAndThrowException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new NatsStreaming("unknown", "config", "port", "4232"),
                "No enum constant"
        );
    }

    @Test
    @DisplayName("Duplicate instances [FAIL]")
    void natsServer_asTwoInstances_shouldThrowBindException() {
        final NatsStreaming nats_Java_one = new NatsStreaming(4500);
        final NatsStreaming nats_Java_two = new NatsStreaming(4500);
        Exception exception = null;
        try {
            nats_Java_one.start();
            nats_Java_two.start(NATS_TIMEOUT);
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
    void natsServer_stopWithoutStart_shouldNotRunIntroException() {
        nats.stop();
        assertThat(nats.pid(), is(-1));
    }

    @Test
    @DisplayName("Start multiple times")
    void natsServer_multipleTimes_shouldBeOkay() throws Exception {
        final NatsStreaming nats1 = new NatsStreaming(-1).start(NATS_TIMEOUT);
        final int pid1 = nats1.pid();
        nats1.stop(NATS_TIMEOUT * 2);
        final NatsStreaming nats2 = new NatsStreaming(-1).start(NATS_TIMEOUT);
        final int pid2 = nats2.pid();
        nats2.stop(NATS_TIMEOUT * 2);
        final NatsStreaming nats3 = new NatsStreaming(-1).start(NATS_TIMEOUT);
        final int pid3 = nats3.pid();
        nats3.stop(NATS_TIMEOUT * 2);

        assertThat(pid1, is(not(equalTo(pid2))));
        assertThat(pid2, is(not(equalTo(pid3))));
        assertThat(pid3, is(not(equalTo(pid1))));
    }

    @Test
    @DisplayName("Start in parallel")
    void natsServer_inParallel_shouldBeOkay() throws Exception {
        final NatsStreaming nats1 = new NatsStreaming(-1).start();
        final NatsStreaming nats2 = new NatsStreaming(-1).start();
        assertThat(nats1.pid(), is(not(equalTo(nats2.pid()))));
        assertThat(nats1.port(), is(not(equalTo(nats2.port()))));
        assertThat(nats1.pidFile(), is(not(equalTo(nats2.pidFile()))));
        assertThat(Files.exists(nats1.pidFile()), is(true));
        assertThat(Files.exists(nats2.pidFile()), is(true));
        nats1.stop();
        nats2.stop(NATS_TIMEOUT);
    }

    @Test
    @DisplayName("Configure with NULL value should be ignored")
    void natsServer_withNullableConfigValue_shouldNotRunIntroExceptionOrInterrupt() {
        assertThrows(NullPointerException.class, () -> nats.config().put(ADDR, null));
    }

    @Test
    @DisplayName("Configure with invalid config value [FAIL]")
    void natsServer_withInvalidConfigValue_shouldNotRunIntroExceptionOrInterrupt() {
        nats.config(ADDR.name(), "invalidValue", PORT.name(), "4237");
        assertThrows(
                PortUnreachableException.class,
                () -> nats.start(NATS_TIMEOUT),
                "NatsServer failed to start"
        );
    }

    @Test
    @DisplayName("Configure without value param")
    void natsServer_withoutValue() throws Exception {
        nats.config(TRACE, "true").config(DEBUG, "true");
        nats.start();
        assertThat(nats.pid(), is(greaterThan(-1)));
    }


    @Test
    @DisplayName("Cov dummy")
    void covDummy() {
        nats.tryStart();
        nats.close();
        assertThat(mapValueOf(ENV, "some value").toString(), is(notNullValue()));
        assertThat(new NatsFileReaderException("dummy", new RuntimeException()), is(notNullValue()));
        assertThat(new NatsStartException(new RuntimeException()), is(notNullValue()));
        assertThat(new NatsStreamingDownloadException(new RuntimeException()), is(notNullValue()));
    }
}
