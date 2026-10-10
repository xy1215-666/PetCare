package io.petcare.backend.ai.exception;

public class AiProviderException extends RuntimeException {

    private static final String PUBLIC_MESSAGE = "智能助手暂时不可用，请稍后再试";

    public AiProviderException() {
        super(PUBLIC_MESSAGE);
    }

    public AiProviderException(Throwable cause) {
        super(PUBLIC_MESSAGE, cause);
    }
}
