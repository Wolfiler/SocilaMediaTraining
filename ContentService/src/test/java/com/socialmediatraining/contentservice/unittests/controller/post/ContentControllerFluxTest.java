package com.socialmediatraining.contentservice.unittests.controller.post;

import com.socialmediatraining.contentservice.controller.post.ContentController;
import com.socialmediatraining.contentservice.dto.post.ContentResponse;
import com.socialmediatraining.contentservice.service.post.ContentService;
import com.socialmediatraining.dtoutils.dto.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@WebFluxTest(ContentController.class)
class ContentControllerFluxTest {

    private static final String BASE_URI = "/api/v1/feed";
    private final static String VALID_HEADER = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibm" +
            "FtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMiwicHJlZmVycmVkX3VzZXJuYW1lIjoidXNlci5zb2NpY" +
            "WxtZWRpYSJ9.l3I61dPRuMr3zqXoG3TqOAJ0_4tnf3z_LcU4TtUM4fw";

    @Autowired
    private WebTestClient webTestClient;
    @MockitoBean
    private ContentService contentService;
    private ContentResponse testResponse1;
    private ContentResponse testResponse2;

    @BeforeEach
    void setUp() {
        testResponse1 = ContentResponse.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDateTime.parse("2023-01-01T10:00:00"),
                LocalDateTime.now(),
                "Test post 1",
                null
        );

        testResponse2 = ContentResponse.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDateTime.parse("2023-01-02T10:00:00"),
                LocalDateTime.now(),
                "Test post 2",
                null
        );
    }

    @Test
    void getUserFeed_WithValidRequest_Returns200() {
        PageResponse<ContentResponse> pageResponse = new PageResponse<>(
                List.of(testResponse1, testResponse2), 0, 1, 2, 2);

        given(contentService.getUserFeed(anyString(), anyString(), any(Pageable.class)))
                .willReturn(Flux.just(pageResponse));

        webTestClient.get()
                .uri(uri -> uri.path(BASE_URI)
                        .queryParam("page", "0")
                        .queryParam("size", "10")
                        .build())
                .header("Authorization", VALID_HEADER)
                .exchange()
                .expectStatus().isOk()

                .expectBodyList(PageResponse.class)
                .value(result -> {
                    assertThat(result.size()).isEqualTo(1);
                    assertThat(result.getFirst().content().size()).isEqualTo(2);
                    assertThat(result.getFirst().totalItems()).isEqualTo(2);
                    assertThat(result.getFirst().currentPage()).isZero();
                    assertThat(result.getFirst().totalPages()).isEqualTo(1);
                });
    }

    @Test
    void getUserFeed_WithPagination_ReturnsCorrectPage() {
        PageResponse<ContentResponse> pageResponse = new PageResponse<>(
                List.of(testResponse2),
                1, 2, 2, 1
        );
        Pageable pageable = PageRequest.of(1, 1);

        given(contentService.getUserFeed(anyString(), anyString(), eq(pageable)))
                .willReturn(Flux.just(pageResponse));

        webTestClient.get()
                .uri(uri -> uri.path(BASE_URI)
                        .queryParam("page", "1")
                        .queryParam("size", "1")
                        .build())
                .header("Authorization", VALID_HEADER)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].content[0].id").isEqualTo(testResponse2.id().toString())
                .jsonPath("$[0].content[0].text").isEqualTo(testResponse2.text())
                .jsonPath("$[0].currentPage").isEqualTo(1)
                .jsonPath("$[0].totalPages").isEqualTo(2)
                .jsonPath("$[0].totalItems").isEqualTo(2)
                .jsonPath("$[0].size").isEqualTo(1);
    }

    @Test
    void getUserFeed_WhenUnauthorized_Returns400() {
        webTestClient.get()
                .uri(BASE_URI)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void getUserFeed_WithInvalidPagination_Returns400() {
        webTestClient.get()
                .uri(uri -> uri.path(BASE_URI)
                        .queryParam("page", "-1")
                        .queryParam("size", "10")
                        .build())
                .header("Authorization", VALID_HEADER)
                .exchange()
                .expectStatus().isBadRequest();

        webTestClient.get()
                .uri(uri -> uri.path(BASE_URI)
                        .queryParam("page", "0")
                        .queryParam("size", "0")
                        .build())
                .header("Authorization", VALID_HEADER)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void getUserFeed_WhenNoContent_ReturnsEmptyPage() {
        PageResponse<ContentResponse> emptyPageResponse = new PageResponse<>(
                List.of(), 0, 0, 0, 0
        );

        given(contentService.getUserFeed(anyString(), anyString(), any(Pageable.class)))
                .willReturn(Flux.just(emptyPageResponse));

        webTestClient.get()
                .uri(uri -> uri.path(BASE_URI)
                        .queryParam("page", "0")
                        .queryParam("size", "10")
                        .build())
                .header("Authorization", VALID_HEADER)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].content").isArray()
                .jsonPath("$[0].content.length()").isEqualTo(0)
                .jsonPath("$[0].totalItems").isEqualTo(0)
                .jsonPath("$[0].totalPages").isEqualTo(0);
    }

    @Test
    void getUserFeed_WhenServiceThrowsException_Returns500() {
        given(contentService.getUserFeed(anyString(), anyString(), any(Pageable.class)))
                .willReturn(Flux.error(new RuntimeException("Database connection failed")));

        webTestClient.get()
                .uri(uri -> uri.path(BASE_URI)
                        .queryParam("page", "0")
                        .queryParam("size", "10")
                        .build())
                .header("Authorization", VALID_HEADER)
                .exchange()
                .expectStatus().is5xxServerError();
    }
}