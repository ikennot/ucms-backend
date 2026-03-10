package com.ucms_backend.controller;

import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ControllerRequestBodyValidationAnnotationTest {

    @Test
    void allRequestBodyParameters_areAnnotatedWithValid() {
        List<Class<?>> controllers = List.of(
                AuthController.class,
                UserController.class,
                TicketController.class,
                TicketResponseController.class,
                CategoryController.class,
                NotificationController.class,
                AttachmentController.class,
                AnalyticsController.class
        );

        List<String> missingValidAnnotations = new ArrayList<>();

        for (Class<?> controller : controllers) {
            for (Method method : controller.getDeclaredMethods()) {
                for (Parameter parameter : method.getParameters()) {
                    if (!parameter.isAnnotationPresent(RequestBody.class)) {
                        continue;
                    }

                    if (!parameter.isAnnotationPresent(Valid.class)) {
                        missingValidAnnotations.add(
                                controller.getSimpleName() + "#" + method.getName() + "(" + parameter.getName() + ")"
                        );
                    }
                }
            }
        }

        assertTrue(
                missingValidAnnotations.isEmpty(),
                "Missing @Valid on @RequestBody parameters: " + missingValidAnnotations
        );
    }
}
