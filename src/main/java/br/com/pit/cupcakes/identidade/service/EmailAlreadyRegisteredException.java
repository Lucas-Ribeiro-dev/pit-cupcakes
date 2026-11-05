package br.com.pit.cupcakes.identidade.service;

public class EmailAlreadyRegisteredException extends RuntimeException {
    public EmailAlreadyRegisteredException() {
        super("E-mail já cadastrado.");
    }
}
