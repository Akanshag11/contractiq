package com.contractiq.exception;

public class InvalidContractStateException extends RuntimeException{
    public InvalidContractStateException(String message) {
        super(message);
    }
}
