package demo;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}

@RestController
class GreetingsHttpController {

    private final Logger log = LoggerFactory.getLogger(GreetingsHttpController.class);

    private final ObservationRegistry registry;

    GreetingsHttpController(ObservationRegistry registry) {
        this.registry = registry;
    }

    @GetMapping("/greetings/{name}")
    Greeting greet(@PathVariable String name, HttpServletRequest request) {
        if (log.isInfoEnabled()) {
            request.getAttributeNames().asIterator().forEachRemaining(attributeName -> log.info("attributeName: " + attributeName));
        }
        if (!Character.isUpperCase(name.charAt(0))) {
            throw new IllegalArgumentException("you need to provide a name!");
        }
        return Observation
                .createNotStarted("greetings", this.registry)
                .observe(() -> new Greeting("Hello, " + name + "!"));
    }

    @ExceptionHandler
    ResponseEntity<ProblemDetail> handleException(IllegalArgumentException exception) {
        if (log.isInfoEnabled()) {
            log.info("got an exception {}", exception.getMessage());
        }
        var pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("you need to provide a valid name");
        return ResponseEntity.badRequest().body(pd);
    }
}

record Greeting(String name) {
}
