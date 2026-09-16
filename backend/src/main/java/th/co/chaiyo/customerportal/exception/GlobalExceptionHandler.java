package th.co.chaiyo.customerportal.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;

import reactor.core.publisher.Mono;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomerNotFoundException.class)
    public Mono<ResponseEntity<ApiError>> handleNotFound(CustomerNotFoundException ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(ex.getCode(), ex.getMessage(), ex.getDescription())));
    }

    @ExceptionHandler(Customer360UnavailableException.class)
    public Mono<ResponseEntity<ApiError>> handleUpstreamUnavailable(Customer360UnavailableException ex) {
        log.error("{}: {}", ex.getCode(), ex.getDescription(), ex.getCause());
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiError(ex.getCode(), ex.getMessage(), ex.getDescription())));
    }

    @ExceptionHandler(ProfileVersionConflictException.class)
    public Mono<ResponseEntity<ApiError>> handleVersionConflict(ProfileVersionConflictException ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(ex.getCode(), ex.getMessage(), ex.getDescription())));
    }

    @ExceptionHandler({WebExchangeBindException.class, ServerWebInputException.class})
    public Mono<ResponseEntity<ApiError>> handleBadRequest(Exception ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("VF001", "Invalid request", ex.getMessage())));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ApiError>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("SF001", "Internal server error", "An unexpected error occurred")));
    }
}
