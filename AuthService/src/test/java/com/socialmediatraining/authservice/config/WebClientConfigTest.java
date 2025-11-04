package com.socialmediatraining.authservice.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.reactive.function.client.WebClient;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
class WebClientConfigTest {

    private WebClientConfig webClientConfig;
    private WebClient webClient;

    @BeforeEach
    public void setUp() {
        webClientConfig = new WebClientConfig();
        webClient = webClientConfig.webClient().build();
    }

    @Test
    public void WebClientConfig_should_be_correctly_instantiated() {
        assertThat(webClient).isNotNull();
        assertThat(webClient).isInstanceOf(WebClient.class);
    }

}