package berlin.yuna.natsserver.model.exception;

public class NatsStreamingFileReaderException extends RuntimeException {

    public NatsStreamingFileReaderException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
