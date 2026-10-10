package io.petcare.backend.ai.exception;

public class AiUnavailableException extends RuntimeException {

    public AiUnavailableException() {
        super("智能助手当前未启用");
    }
}
