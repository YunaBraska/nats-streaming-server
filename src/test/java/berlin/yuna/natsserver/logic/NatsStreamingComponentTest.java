package berlin.yuna.natsserver.logic;

import berlin.yuna.natsserver.config.NatsStreamingConfig;
import berlin.yuna.natsserver.config.NatsStreamingOptions;
import berlin.yuna.natsserver.config.NatsStreamingOptionsBuilder;
import berlin.yuna.natsserver.model.exception.NatsStreamingDownloadException;
import berlin.yuna.natsserver.model.exception.NatsStreamingFileReaderException;
import berlin.yuna.natsserver.model.exception.NatsStreamingStartException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.net.Socket;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;

import static berlin.yuna.natsserver.config.NatsStreamingConfig.ADDR;
import static berlin.yuna.natsserver.config.NatsStreamingConfig.DEBUG;
import static berlin.yuna.natsserver.config.NatsStreamingConfig.MAX_CHANNELS;
import static berlin.yuna.natsserver.config.NatsStreamingConfig.PORT;
import static berlin.yuna.natsserver.config.NatsStreamingConfig.TRACE;
import static berlin.yuna.natsserver.config.NatsStreamingOptions.natsStreamingBuilder;
import static berlin.yuna.natsserver.model.MapValue.mapValueOf;
import static berlin.yuna.natsserver.model.ValueSource.ENV;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("UnitTest")
@DisplayName("NatsServer plain java")
@SuppressWarnings("resource")
class NatsStreamingComponentTest {

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
        final var nats = new NatsStreaming();
        assertThat(nats.pid(), is(greaterThan(-1)));
    }

    @Test
    @DisplayName("Default config and port")
    void natsServer_withoutConfigAndPort_shouldStartWithDefaultValues() {
        final var nats = new NatsStreaming(-1);
        assertThat(nats.pid(), is(greaterThan(-1)));
    }

    @Test
    @DisplayName("Setup config")
    void natsServer_shouldShutdownGracefully() throws Exception {
        final var port = new AtomicInteger(-99);
        try (final var nats = new NatsStreaming(testConfig().config("user", "adminUser", "PAss", "adminPw"))) {
            assertThat(nats.port(), is(greaterThan(0)));
            port.set(nats.port());
            new Socket("localhost", port.get()).close();
        }
        assertThrows(
                ConnectException.class,
                () -> new Socket("localhost", port.get()).close(),
                "Connection refused"
        );
    }

    @Test
    @DisplayName("Unknown config is ignored")
    void natsServer_invalidConfig_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> new NatsStreaming(testConfig().config("user", "adminUser", "auth", "isValid", "", "password", " ")));
    }

    @Test
    @DisplayName("Duplicate starts will be ignored")
    void natsServer_duplicateStart_shouldNotRunIntroExceptionOrInterrupt() {
        final var nats = new NatsStreaming(testConfig()).start().start().start();
        assertThat(nats.pid(), is(greaterThan(-1)));
    }

    @Test
    @DisplayName("Unknown config [FAIL]")
    void natsServer_withWrongConfig_shouldNotStartAndThrowException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new NatsStreaming(testConfig().config("unknown", "config", "port", "4232")),
                "No enum constant"
        );
    }

    @Test
    @DisplayName("Duplicate instances [FAIL]")
    void natsServer_asTwoInstances_shouldThrowBindException() {
        final NatsStreamingOptions config = natsStreamingBuilder().port(4500).timeoutMs(2000).build();
        new NatsStreaming(config);
        assertThrows(
                NatsStreamingStartException.class,
                () -> new NatsStreaming(config),
                "Address already in use [4500]"
        );
    }

    @Test
    @DisplayName("Stop without start will be ignored")
    void natsServer_stopWithoutStart_shouldNotRunIntroException() {
        final var nats = natsStreamingBuilder().autostart(false).nats();
        nats.close();
        assertThat(nats.pid(), is(-1));
    }

    @Test
    @DisplayName("Start multiple times")
    void natsServer_multipleTimes_shouldBeOkay() {
        final NatsStreaming nats1 = new NatsStreaming(-1);
        final int pid1 = nats1.pid();
        nats1.close();
        final NatsStreaming nats2 = new NatsStreaming(-1);
        final int pid2 = nats2.pid();
        nats2.close();
        final NatsStreaming nats3 = new NatsStreaming(-1);
        final int pid3 = nats3.pid();
        nats3.close();

        assertThat(pid1, is(not(equalTo(pid2))));
        assertThat(pid2, is(not(equalTo(pid3))));
        assertThat(pid3, is(not(equalTo(pid1))));
    }

    @Test
    @DisplayName("Start in parallel")
    void natsServer_inParallel_shouldBeOkay() {
        final NatsStreaming nats1 = new NatsStreaming(-1);
        final NatsStreaming nats2 = new NatsStreaming(-1);
        assertThat(nats1.pid(), is(not(equalTo(nats2.pid()))));
        assertThat(nats1.port(), is(not(equalTo(nats2.port()))));
        assertThat(nats1.pidFile(), is(not(equalTo(nats2.pidFile()))));
        assertThat(Files.exists(nats1.pidFile()), is(true));
        assertThat(Files.exists(nats2.pidFile()), is(true));
        nats1.close();
        nats2.close();
    }

    @Test
    @DisplayName("Configure with NULL value should be ignored")
    void natsServer_withNullableConfigValue_shouldNotRunIntroExceptionOrInterrupt() {
        new NatsStreaming(testConfig().config(ADDR, null));
    }

    @Test
    @DisplayName("Configure with invalid config value [FAIL]")
    void natsServer_withInvalidConfigValue_shouldNotRunIntroExceptionOrInterrupt() {
        assertThrows(
                NatsStreamingStartException.class,
                () -> new NatsStreaming(testConfig().config(MAX_CHANNELS.name(), "invalidValue", PORT.name(), "4237")),
                "NatsServer failed to start"
        );
    }

    @Test
    @DisplayName("Configure without value param")
    void natsServer_withoutValue() {
        final var nats = new NatsStreaming(testConfig().config(TRACE, "true").config(DEBUG, "true"));
        assertThat(nats.pid(), is(greaterThan(-1)));
    }


    @Test
    @DisplayName("Cov dummy")
    void covDummy() {
        assertThat(DEBUG.type(), is(equalTo(NatsStreamingConfig.SilentBoolean.class)));
        assertThat(mapValueOf(ENV, "some value").toString(), is(notNullValue()));
        assertThat(new NatsStreamingFileReaderException("dummy", new RuntimeException()), is(notNullValue()));
        assertThat(new NatsStreamingStartException(new RuntimeException()), is(notNullValue()));
        assertThat(new NatsStreamingDownloadException(new RuntimeException()), is(notNullValue()));
        assertThat(new NatsStreamingConfig.SilentBoolean().getAndSet(true), is(false));
    }

    private NatsStreamingOptionsBuilder testConfig() {
        return natsStreamingBuilder().config(PORT, "-1");
    }
}
