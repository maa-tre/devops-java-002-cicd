package com.example.devoopsclass;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RollbackControllerTests {
    @Test
    void locksAddressAfterFiveIncorrectPins() {
        RollbackController controller = new RollbackController(
                mock(RollbackRequestRepository.class),
                new RollbackPinVerifier(""),
                "http://127.0.0.1:8790",
                "");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("192.0.2.10");
        RollbackController.RollbackCommand command =
                new RollbackController.RollbackCommand("java-app:ci-1", "00000000");

        for (int attempt = 0; attempt < 5; attempt++) {
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> controller.requestRollback(command, request));
            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        }

        ResponseStatusException locked = assertThrows(
                ResponseStatusException.class,
                () -> controller.requestRollback(command, request));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, locked.getStatusCode());
    }
}
