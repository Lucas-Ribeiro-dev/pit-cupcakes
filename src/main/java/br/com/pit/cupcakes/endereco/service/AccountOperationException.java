package br.com.pit.cupcakes.endereco.service;

public class AccountOperationException extends RuntimeException {
    private final int status;
    private final String code;
    public AccountOperationException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
    public int getStatus() { return status; }
    public String getCode() { return code; }
}
