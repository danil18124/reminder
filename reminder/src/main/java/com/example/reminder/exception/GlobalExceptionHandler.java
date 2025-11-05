package com.example.reminder.exception;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import com.example.reminder.model.dto.ApiErrorResponse;

import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

//    ReminderNotFoundException - сущность не найдена
//    MethodArgumentNotValidException - @Valid @RequestBody
//    InvalidPageRequestException - некорректные параметры пагинации. Не срабатывает, потому что Spring Data автоматически 
//                                заменяет невалидные параметры на дефолтные значения
//    InvalidDateRangeException - некорректный диапазон дат
//    ConstraintViolationException - устаревшее исключение. убрал его и добавил HandlerMethodValidationException
//    HandlerMethodValidationException - RequestParam("title") @NotBlank(message = "Title must not be empty") String title
    @ExceptionHandler(ReminderNotFoundException.class)
    public ResponseEntity<ApiErrorResponse<Object>> handleReminderNotFound(ReminderNotFoundException ex) {
        ApiErrorResponse<Object> error = new ApiErrorResponse<>(
                ex.getErrorCode(),
                ex.getMessage(),
                Map.of("reminderId", ex.getReminderId())
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse<List<String>>> handleValidationErrors(MethodArgumentNotValidException ex) {
        return buildValidationErrorResponse(
                ex.getBindingResult().getFieldErrors()
                        .stream()
                        .collect(Collectors.toMap(
                                err -> err.getField(),
                                err -> List.of(err.getDefaultMessage()),
                                (list1, list2) -> { // если несколько ошибок на поле
                                    List<String> merged = new ArrayList<>(list1);
                                    merged.addAll(list2);
                                    return merged;
                                }
                        ))
        );
    }

    private ResponseEntity<ApiErrorResponse<List<String>>> buildValidationErrorResponse(Map<String, List<String>> details) {
        ApiErrorResponse<List<String>> error = new ApiErrorResponse<>(
                "VALIDATION_ERROR",
                "Invalid input data",
                details
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse<Object>> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        ApiErrorResponse<Object> error = new ApiErrorResponse<>(
                "INTERNAL_SERVER_ERROR",
                "Unexpected error occurred",
                Map.of("exception", ex.getClass().getSimpleName())
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(InvalidPageRequestException.class)
    public ResponseEntity<ApiErrorResponse<Object>> handleInvalidPageRequest(InvalidPageRequestException ex) {
        ApiErrorResponse<Object> error = new ApiErrorResponse<>(
                ex.getErrorCode(),
                ex.getMessage(),
                Map.of("page", ex.getPage(), "size", ex.getSize())
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(InvalidDateRangeException.class)
    public ResponseEntity<ApiErrorResponse<Object>> handleInvalidDateRange(InvalidDateRangeException ex) {
        ApiErrorResponse<Object> error = new ApiErrorResponse<>(
                ex.getErrorCode(),
                ex.getMessage(),
                Map.of("start", ex.getStart(), "end", ex.getEnd())
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiErrorResponse<List<String>>> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        Map<String, List<String>> details = new HashMap<>();

        ex.getAllValidationResults().forEach(result
                -> result.getResolvableErrors().forEach(error -> {
                    String field = result.getMethodParameter().getParameterName(); // "title"
                    details.computeIfAbsent(field, k -> new ArrayList<>()).add(error.getDefaultMessage());
                })
        );

        ApiErrorResponse<List<String>> error = new ApiErrorResponse<>(
                "VALIDATION_ERROR",
                "Invalid input data",
                details
        );

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse<String> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, String> details = Map.of(
                ex.getName(), "Invalid value: " + ex.getValue()
        );
        return new ApiErrorResponse<>(
                "INVALID_PARAMETER",
                "Invalid parameter: " + ex.getName(),
                details
        );
    }

}
