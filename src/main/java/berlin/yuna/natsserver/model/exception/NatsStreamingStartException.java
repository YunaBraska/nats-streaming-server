package berlin.yuna.natsserver.model.exception;

public class NatsStreamingStartException extends RuntimeException {

    public NatsStreamingStartException(final Throwable cause) {
        super(cause);
    }
}
