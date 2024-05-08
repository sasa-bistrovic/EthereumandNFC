package com.by.sasa.bistrovic.ethereumandnfc.ethereum.exeptions;

public class InvalidEthereumAddressException extends Exception {

    public InvalidEthereumAddressException() {
    }

    public InvalidEthereumAddressException(String message) {
        super(message);
    }

    public InvalidEthereumAddressException(String message, Throwable cause) {
        super(message, cause);
    }

}