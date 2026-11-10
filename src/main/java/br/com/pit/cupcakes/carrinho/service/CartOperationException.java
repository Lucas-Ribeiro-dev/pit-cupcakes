package br.com.pit.cupcakes.carrinho.service;

public class CartOperationException extends RuntimeException {
    private final int status;
    private final String code;

    public CartOperationException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int getStatus() { return status; }
    public String getCode() { return code; }
}
