package com.mawa3id.common;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Direct unit tests for {@link GlobalExceptionHandler}. In particular this covers
 * the generic {@code Exception -> 500} branch, which no integration test exercises
 * (all real endpoints fail with mapped statuses, never an unexpected error).
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private HttpServletRequest requestTo(String uri) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn(uri);
        return req;
    }

    @Test
    void handleGenericMapsToInternalServerError() {
        ResponseEntity<ApiError> response =
                handler.handleGeneric(new RuntimeException("boom"), requestTo("/api/doctors"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        // The raw exception message must not leak to clients.
        assertThat(response.getBody().message()).isEqualTo("Unexpected error");
        assertThat(response.getBody().path()).isEqualTo("/api/doctors");
    }

    @Test
    void handleApiUsesStatusAndMessageFromException() {
        ApiException ex = new ApiException(HttpStatus.CONFLICT, "Email already registered");

        ResponseEntity<ApiError> response = handler.handleApi(ex, requestTo("/api/auth/register/patient"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().message()).isEqualTo("Email already registered");
    }

    @Test
    void handleBadCredentialsMapsToUnauthorizedWithGenericMessage() {
        ResponseEntity<ApiError> response =
                handler.handleBadCredentials(new BadCredentialsException("bad"), requestTo("/api/auth/login"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Invalid email or password");
    }

    @Test
    void handleAccessDeniedMapsToForbidden() {
        ResponseEntity<ApiError> response =
                handler.handleAccessDenied(new AccessDeniedException("nope"), requestTo("/api/patients/me"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Access denied");
    }

    @Test
    void handleUnreadableMapsToBadRequest() {
        ResponseEntity<ApiError> response = handler.handleUnreadable(
                new HttpMessageNotReadableException("bad json"), requestTo("/api/auth/login"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Malformed or missing request body");
    }

    @Test
    void handleTypeMismatchMapsToBadRequestAndNamesParameter() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("id");

        ResponseEntity<ApiError> response = handler.handleTypeMismatch(ex, requestTo("/api/doctors/abc"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("id");
    }

    @Test
    void handleMethodNotSupportedMapsTo405() {
        ResponseEntity<ApiError> response = handler.handleMethodNotSupported(
                new HttpRequestMethodNotSupportedException("POST"), requestTo("/api/specialties"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    void handleUnsupportedMediaTypeMapsTo415() {
        ResponseEntity<ApiError> response = handler.handleUnsupportedMediaType(
                new HttpMediaTypeNotSupportedException("text/plain"), requestTo("/api/auth/login"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void handleNoResourceMapsTo404() {
        ResponseEntity<ApiError> response = handler.handleNoResource(
                new NoResourceFoundException(org.springframework.http.HttpMethod.GET, "/nope"),
                requestTo("/nope"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
    }

    @Test
    void handleValidationCollectsFieldErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("obj", "email", "must be a well-formed email address"),
                new FieldError("obj", "password", "size must be between 8 and 100")));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ApiError> response = handler.handleValidation(ex, requestTo("/api/auth/register/patient"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Validation failed");
        assertThat(response.getBody().fieldErrors())
                .containsEntry("email", "must be a well-formed email address")
                .containsEntry("password", "size must be between 8 and 100");
    }
}
