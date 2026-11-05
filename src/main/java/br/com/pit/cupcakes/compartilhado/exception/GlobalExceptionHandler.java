package br.com.pit.cupcakes.compartilhado.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import br.com.pit.cupcakes.identidade.service.EmailAlreadyRegisteredException;
import jakarta.validation.ValidationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.security.access.AccessDeniedException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(EmailAlreadyRegisteredException ex) {
        return ResponseEntity.status(409).body(new ErrorResponse("EMAIL_JA_CADASTRADO", "E-mail já cadastrado."));
    }

    @ExceptionHandler({ValidationException.class, MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> handleInvalidData(Exception ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("DADOS_INVALIDOS", "Confira os campos informados e os limites permitidos."));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedFormat(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(415).body(new ErrorResponse("FORMATO_NAO_SUPORTADO", "Formato de requisição não suportado."));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleMissingResource(NoResourceFoundException ex) {
        return ResponseEntity.status(404).body(new ErrorResponse("RECURSO_NAO_ENCONTRADO", "Recurso não encontrado."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMethod(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(405).body(new ErrorResponse("METODO_NAO_PERMITIDO", "Método de requisição não permitido."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(403).body(new ErrorResponse("ACESSO_NEGADO", "Acesso não permitido."));
    }

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String CODE_INVALID_ID = "ID_INVALIDO";
    private static final String CODE_NOT_FOUND = "PRODUTO_NAO_ENCONTRADO";
    private static final String CODE_INTERNAL_ERROR = "ERRO_INTERNO";

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProductNotFound(ProductNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(CODE_NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(InvalidIdException.class)
    public ResponseEntity<ErrorResponse> handleInvalidId(InvalidIdException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(CODE_INVALID_ID, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(CODE_INVALID_ID, "O identificador informado deve ser um número inteiro positivo."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex) {
        logger.error("Falha inesperada durante o processamento da requisição", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(CODE_INTERNAL_ERROR, "Ocorreu um erro interno no servidor."));
    }
}

