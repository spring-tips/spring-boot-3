package bootiful.client;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.DecoratingProxy;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Slf4j
@SpringBootApplication
public class ClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }


    @Bean
    ApplicationListener<ApplicationReadyEvent> ready(GreetingsClient client, ObservationRegistry or) {
        return event ->
                Observation
                        .createNotStarted("client.greetings", or)
                        .observe(() -> System.out.println("got a response: " + client.greet( "Spring Fans!" ).message()));
    }

    static class GreetingsClientRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            hints.proxies().registerJdkProxy(GreetingsClient.class, SpringProxy.class, Advised.class, DecoratingProxy.class);
        }
    }

    @Bean
    @RegisterReflectionForBinding(Greeting.class)
    @ImportRuntimeHints(GreetingsClientRuntimeHintsRegistrar.class)
    GreetingsClient greetingsClient(HttpServiceProxyFactory proxyFactory) {
        return proxyFactory.createClient(GreetingsClient.class);
    }

    @Bean
    HttpServiceProxyFactory httpServiceProxyFactory(WebClient.Builder builder) {
        return WebClientAdapter.createHttpServiceProxyFactory(
                builder.baseUrl("http://localhost:8080")
        );
    }
}

interface GreetingsClient {

    @GetExchange("/greetings/{name}")
    Greeting greet(@PathVariable String name);
}

record Greeting(String message) {
}
