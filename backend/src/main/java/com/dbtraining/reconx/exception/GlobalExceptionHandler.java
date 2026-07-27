package com.dbtraining.reconx.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;


/**
 * ============================================================================
 * TICKET-ADV062 — RFC 7807 ProblemDetail for every ReconException
 *
 * Maps each domain exception subtype to the right HTTP status, with a
 * structured ProblemDetail body so clients don't have to parse free text.
 * ============================================================================
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(TradeNotFoundException.class)
    public ProblemDetail notFound(TradeNotFoundException ex) {
        // TODO(TICKET-ADV062): return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setType(URI.create("https://reconx.dbtraining.com/errors/trade-not-found"));
        problemDetail.setTitle("Trade not found");
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(DuplicateTradeRefException.class)
    public ProblemDetail duplicate(DuplicateTradeRefException ex) {
        // TODO(TICKET-ADV062): map DuplicateTradeRefException -> HttpStatus.CONFLICT (409).
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setType(URI.create("https://reconx.dbtraining.com/errors/duplicate-trade-ref"));
        problemDetail.setTitle("Duplicate trade reference");
        return problemDetail;
    }

    @ExceptionHandler(InvalidTradeException.class)
    public ProblemDetail invalid(InvalidTradeException ex) {
        // TODO(TICKET-ADV062): map InvalidTradeException -> HttpStatus.BAD_REQUEST (400).
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setType(URI.create("https://reconx.dbtraining.com/errors/invalid-trade"));
        problemDetail.setTitle("Invalid trade");
        return problemDetail;
    }

    @ExceptionHandler(ReconciliationMismatchException.class)
    public ProblemDetail mismatch(ReconciliationMismatchException ex) {
        // TODO(TICKET-ADV062): map ReconciliationMismatchException -> HttpStatus.UNPROCESSABLE_ENTITY (422).
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problemDetail.setType(URI.create("https://reconx.dbtraining.com/errors/reconciliation-mismatch"));
        problemDetail.setTitle("Reconciliation Mismatch");
        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validation(MethodArgumentNotValidException ex) {
        // TODO(TICKET-ADV062): join field errors ("field: message; ...") and return BAD_REQUEST ProblemDetail.
        //   Hint: ex.getBindingResult().getFieldErrors().stream().map(...).collect(Collectors.joining("; "))
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problemDetail.setTitle("Validation failed");
        return problemDetail;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail constraint(ConstraintViolationException ex) {
        // TODO(TICKET-ADV062): map ConstraintViolationException -> HttpStatus.BAD_REQUEST (400).
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setType(URI.create("https://reconx.dbtraining.com/errors/constraint-violation"));
        problemDetail.setTitle("Constraint Violation");
        return problemDetail;
    }
}
