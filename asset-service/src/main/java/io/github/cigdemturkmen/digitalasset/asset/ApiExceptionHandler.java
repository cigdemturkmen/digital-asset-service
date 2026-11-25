package io.github.cigdemturkmen.digitalasset.asset;

import io.github.cigdemturkmen.digitalasset.asset.wallet.InsufficientBalanceException;
import io.github.cigdemturkmen.digitalasset.asset.wallet.SameWalletTransferException;
import io.github.cigdemturkmen.digitalasset.asset.wallet.WalletNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Comparator;
import java.util.List;

/**
 * Translates exceptions into RFC 9457 problem details ({@code application/problem+json}) for all controllers.
 * The base class already covers Spring MVC's own errors (malformed JSON, invalid UUID in the path, unsupported
 * method, ...). This class adds the domain exceptions, field-level validation errors and a safe fallback.
 * The domain exceptions stay free of HTTP: the mapping to status codes lives only here.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(WalletNotFoundException.class)
    ProblemDetail walletNotFound(WalletNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Wallet not found");
        problem.setProperty("walletId", ex.getWalletId());
        return problem;
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    ProblemDetail insufficientBalance(InsufficientBalanceException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
        problem.setTitle("Insufficient balance");
        problem.setProperty("walletId", ex.getWalletId());
        problem.setProperty("balance", ex.getBalance());
        problem.setProperty("requested", ex.getRequested());
        return problem;
    }

    // 400, not 422: the request is invalid on its own, without looking at any wallet's state.
    @ExceptionHandler(SameWalletTransferException.class)
    ProblemDetail sameWalletTransfer(SameWalletTransferException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Same wallet transfer");
        problem.setProperty("walletId", ex.getWalletId());
        return problem;
    }

    // Retries are exhausted: another request kept changing the same wallet. Safe for the client to retry.
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail concurrentModification(OptimisticLockingFailureException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The wallet was modified by another request at the same time; please retry");
        problem.setTitle("Concurrent modification");
        return problem;
    }

    // Anything unexpected: log the full stack trace, but never send internals (SQL, class names) to the client.
    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    // @Valid failed: list every invalid field instead of the base class's generic "Invalid request content."
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldViolation> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(FieldViolation::field).thenComparing(FieldViolation::message))
                .toList();
        ProblemDetail problem = ex.getBody();
        problem.setDetail("Request validation failed");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    record FieldViolation(String field, String message) {
    }
}
