package com.tonycode2.software.ledger_service.common.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(InsufficientFundsException.class)
    ProblemDetail handle(InsufficientFundsException ex) {
        ProblemDetail pd = constructProblemDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(),
                "Insufficient Funds");
        return pd;
    }

    @ExceptionHandler(InvalidAmountException.class)
    ProblemDetail handle(InvalidAmountException ex) {
        ProblemDetail pd = constructProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), "Invalid Amount");
        return pd;
    }

    @ExceptionHandler(InvalidAccountException.class)
    ProblemDetail handle(InvalidAccountException ex) {
        ProblemDetail pd = constructProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), "Invalid Account");
        return pd;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        ProblemDetail pd = constructProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected Error",
                "Unexpected Error");
        return pd;
    }

    private ProblemDetail constructProblemDetail(HttpStatus status, String message, String title) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, message);
        pd.setTitle(title);
        return pd;
    }

}
