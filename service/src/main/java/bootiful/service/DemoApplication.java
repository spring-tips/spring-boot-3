package bootiful.service;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}

@Slf4j
@RestController
class GreetingsHttpController {

    private final ObservationRegistry registry;

    GreetingsHttpController(ObservationRegistry registry) {
        this.registry = registry;
    }

    @GetMapping("/greetings/{name}")
    Greeting greet(@PathVariable String name, HttpServletRequest request) {

        if (!Character.isUpperCase(name.charAt(0))) {
            throw new IllegalArgumentException("you need to provide a name!");
        }
        return Observation
                .createNotStarted("greetings", this.registry)
                .observe(() -> new Greeting("Hello, " + name + "!"));
    }
}

@Slf4j
@ControllerAdvice
class ProblemDetailsControllerAdvice {

    @ExceptionHandler
    ResponseEntity<ProblemDetail> handleException(
            HttpServletRequest request,
            IllegalArgumentException exception) {
        if (log.isInfoEnabled()) {
            log.info("got an exception {}", exception.getMessage());
            request.getAttributeNames().asIterator()
                    .forEachRemaining(attributeName -> log.info("attributeName: " + attributeName));
        }
        var pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("you need to provide a valid name");
        return ResponseEntity.badRequest().body(pd);
    }

}

record Greeting(String message) {}
