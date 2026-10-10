package com.tonycode2.software.ledger_service.common.exceptions;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(InsufficientFundsException.class)
    ProblemDetail handleInsuficientFounds(InsufficientFundsException ex) {
        ProblemDetail pd = constructProblemDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(),
                "Insufficient Funds");
        return pd;
    }

    @ExceptionHandler(InvalidAmountException.class)
    ProblemDetail handleInvalidAmount(InvalidAmountException ex) {
        ProblemDetail pd = constructProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), "Invalid Amount");
        return pd;
    }

    @ExceptionHandler(InvalidAccountException.class)
    ProblemDetail handleInvalidAccount(InvalidAccountException ex) {
        ProblemDetail pd = constructProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), "Invalid Account");
        return pd;
    }

    @ExceptionHandler(AccountNotFoundException.class)
    ProblemDetail handleAccountNotFound(AccountNotFoundException ex) {
        ProblemDetail pd = constructProblemDetail(HttpStatus.NOT_FOUND, ex.getMessage(), "Account Not Found");
        return pd;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        ProblemDetail pd = constructProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected Error",
                "Unexpected Error");
        return pd;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream().map(error -> Map.of(
                "field", error.getField(),
                "message", String.valueOf(error.getDefaultMessage()))).toList();

        ProblemDetail pd = ex.getBody();
        pd.setTitle("Validation Failed");
        pd.setDetail("Request validation failed");
        pd.setProperty("errors", errors);

        return handleExceptionInternal(ex, pd, headers, status, request);

    }

    private ProblemDetail constructProblemDetail(HttpStatus status, String message, String title) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, message);
        pd.setTitle(title);
        return pd;
    }

}
