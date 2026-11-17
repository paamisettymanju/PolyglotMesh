package com.infotact.polyglotmesh.execution;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.cors.CorsConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "PT2M")
class ExecutionApiTest {
    @Autowired
    private WebTestClient client;

    @Autowired
    private CorsConfiguration corsConfiguration;

    @Test
    void healthEndpointReportsApplicationStatus() {
        client.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    void advertisesOnlySupportedLanguages() {
        client.get().uri("/api/languages")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .json("""
                        ["Python","JavaScript"]
                        """);
    }

    @Test
    void executesPythonThroughTheHttpApi() {
        client.post().uri("/api/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ExecutionRequest("python", "print('Hello from Python!')"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.language").isEqualTo("python")
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.output").isEqualTo("Hello from Python!\n");
    }

    @Test
    void executesJavaScriptThroughTheHttpApi() {
        client.post().uri("/api/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ExecutionRequest(
                        "javascript", "console.log('Hello from JavaScript!');"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.language").isEqualTo("javascript")
                .jsonPath("$.error").value(error ->
                        org.assertj.core.api.Assertions.assertThat(error).as("GraalJS error")
                                .isNull())
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.output").isEqualTo("Hello from JavaScript!\n");
    }

    @Test
    void supportsJavaScriptAliasAndReportsExecutionTime() {
        client.post().uri("/api/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ExecutionRequest("js", "console.log('alias works');"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.language").isEqualTo("javascript")
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.output").isEqualTo("alias works\n")
                .jsonPath("$.executionTimeMs").value(value ->
                        org.assertj.core.api.Assertions.assertThat(((Number) value).longValue())
                                .isGreaterThanOrEqualTo(0));
    }

    @Test
    void treatsSuccessfulEmptyOutputAsSuccess() {
        client.post().uri("/api/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ExecutionRequest("python", "pass"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.output").isEqualTo("")
                .jsonPath("$.error").value(value ->
                        org.assertj.core.api.Assertions.assertThat(value).isNull());
    }

    @Test
    void reportsPythonSyntaxErrorsAsExecutionFailures() {
        client.post().uri("/api/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ExecutionRequest("python", "if True print('invalid')"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.output").isEqualTo("")
                .jsonPath("$.error").value(value ->
                        org.assertj.core.api.Assertions.assertThat(value.toString()).isNotBlank());
    }

    @Test
    void reportsJavaScriptRuntimeErrorsAsExecutionFailures() {
        client.post().uri("/api/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ExecutionRequest("javascript", "throw new Error('expected failure');"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error").value(value ->
                        org.assertj.core.api.Assertions.assertThat(value.toString())
                                .contains("expected failure"));
    }

    @Test
    void createsAFreshContextForEveryExecution() {
        ExecutionRequest request = new ExecutionRequest(
                "python", "counter = globals().get('counter', 0) + 1\nprint(counter)");

        for (int attempt = 0; attempt < 2; attempt++) {
            client.post().uri("/api/execute")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.output").isEqualTo("1\n");
        }
    }

    @Test
    void deniesGuestAccessToHostJavaClasses() {
        client.post().uri("/api/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ExecutionRequest(
                        "javascript",
                        "console.log(Java.type('java.lang.System').getProperty('user.name'));"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.output").isEqualTo("")
                .jsonPath("$.error").value(value ->
                        org.assertj.core.api.Assertions.assertThat(value.toString()).isNotBlank());
    }

    @Test
    void deniesGuestAccessToLocalFiles(@org.junit.jupiter.api.io.TempDir Path tempDir)
            throws IOException {
        String secret = "local-file-content-must-not-be-exposed";
        Path secretFile = Files.writeString(tempDir.resolve("secret.txt"), secret);
        String code = "print(open(r'" + secretFile.toString().replace("\\", "/")
                + "').read())";

        client.post().uri("/api/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ExecutionRequest("python", code))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.output").value(value ->
                        org.assertj.core.api.Assertions.assertThat(value.toString())
                                .doesNotContain(secret))
                .jsonPath("$.error").value(value ->
                        org.assertj.core.api.Assertions.assertThat(value.toString())
                                .doesNotContain(secret));
    }

    @Test
    void boundsCapturedOutput() {
        client.post().uri("/api/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ExecutionRequest("python", "print('x' * 70000)"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.output").value(value ->
                        org.assertj.core.api.Assertions.assertThat(value.toString()).hasSize(64_000))
                .jsonPath("$.error").value(value ->
                        org.assertj.core.api.Assertions.assertThat(value.toString())
                                .contains("Output exceeded the 64000 byte limit."));
    }

    @Test
    void stopsAnInfiniteJavaScriptLoopAtTheResourceLimit() {
        client.post().uri("/api/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ExecutionRequest("javascript", "while (true) {}"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error").isEqualTo("Execution exceeded the resource limit.");
    }

    @Test
    void reportsPythonResourceExhaustionWithoutExposingRuntimeInternals() {
        client.post().uri("/api/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ExecutionRequest("python", "while True:\n    pass"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error").isEqualTo("Execution exceeded the resource limit.")
                .jsonPath("$.error").value(value ->
                        org.assertj.core.api.Assertions.assertThat(value.toString())
                                .doesNotContain("GIL", "AssertionError"));
    }

    @Test
    void rejectsUnsupportedLanguageWithClearValidationError() {
        client.post().uri("/api/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"language":"ruby","code":"puts 'hello'"}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").value(error ->
                        org.assertj.core.api.Assertions.assertThat(error.toString())
                                .contains("language must be python, javascript, or js."));
    }

    @Test
    void rejectsMissingCodeWithAValidationError() {
        client.post().uri("/api/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"language":"python"}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").value(error ->
                        org.assertj.core.api.Assertions.assertThat(error.toString())
                                .contains("code is required."));
    }

    @Test
    void rejectsCodeOverTheDocumentedLimit() {
        String oversizedCode = "x".repeat(PolyglotExecutionService.MAX_SOURCE_LENGTH + 1);
        client.post().uri("/api/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ExecutionRequest("python", oversizedCode))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").value(error ->
                        org.assertj.core.api.Assertions.assertThat(error.toString())
                                .contains("code must not exceed 50000 characters."));
    }

    @Test
    void rejectsMalformedJsonWithAConsistentErrorShape() {
        client.post().uri("/api/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo(
                        "Request body must be valid JSON with a language and code.");
    }

    @Test
    void allowsConfiguredFrontendOrigin() {
        org.assertj.core.api.Assertions.assertThat(
                corsConfiguration.checkOrigin("http://localhost:5173"))
                .isEqualTo("http://localhost:5173");
    }

    @Test
    void doesNotAllowUnconfiguredFrontendOrigin() {
        org.assertj.core.api.Assertions.assertThat(
                corsConfiguration.checkOrigin("https://untrusted.example"))
                .isNull();
    }
}
