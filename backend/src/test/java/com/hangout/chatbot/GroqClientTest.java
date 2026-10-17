package com.hangout.chatbot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hangout.chatbot.AiServiceException.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GroqClientTest {
    
    private static final String URL = "https://api.groq.test/openai/v1/chat/completions";
    private static final String FAKE_KEY = "test-key-not-real";
    private static final List<Map<String, String>> MESSAGES = List.of(
        Map.of("role", "system", "content", "..."),
        Map.of("role", "user", "content", "Une table pour 2 demain à 20h"));
    
    private MockRestServiceServer server;
    private GroqProperties properties;
    private GroqClient client;
    
    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.groq.test/openai/v1");
        server = MockRestServiceServer.bindTo(builder).build();
        properties = new GroqProperties();
        properties.setApiKey(FAKE_KEY);
        client = new GroqClient(builder.build(), properties, new PromptFactory(), new ObjectMapper());
    }
    
    private static String completion(String content, String finishReason) {
        return """
            {"choices":[{"index":0,"finish_reason":"%s","message":{"role":"assistant","content":%s}}]}
            """.formatted(finishReason, new com.fasterxml.jackson.databind.node.TextNode(content).toString());
    }
    
    @Test
    void sendsStrictJsonSchemaRequestAndParsesExtraction() {
        server.expect(requestTo(URL))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer " + FAKE_KEY))
            .andExpect(jsonPath("$.model").value("openai/gpt-oss-20b"))
            .andExpect(jsonPath("$.temperature").value(0))
            .andExpect(jsonPath("$.response_format.type").value("json_schema"))
            .andExpect(jsonPath("$.response_format.json_schema.strict").value(true))
            .andExpect(jsonPath("$.response_format.json_schema.schema.additionalProperties").value(false))
            .andExpect(jsonPath("$.reasoning_effort").value("low"))
            .andExpect(jsonPath("$.messages[1].content").value("Une table pour 2 demain à 20h"))
            .andRespond(withSuccess(completion("""
                {"intent":"CREATE","restaurantName":null,"date":"2026-10-16","time":"20:00","partySize":2,
                 "reservationNumber":null,"targetDate":null,"specialRequests":null,"infoTopic":null}""", "stop"),
                MediaType.APPLICATION_JSON));
        
        ExtractedRequest result = client.extract(MESSAGES);
        
        assertThat(result.chatIntent()).isEqualTo(ChatIntent.CREATE);
        assertThat(result.date()).isEqualTo("2026-10-16");
        assertThat(result.time()).isEqualTo("20:00");
        assertThat(result.partySize()).isEqualTo(2);
        server.verify();
    }
    
    @Test
    void usesPlainJsonModeAndNoReasoningParamsForOtherModels() {
        properties.setModel("llama-3.3-70b-versatile");
        properties.setStructuredOutput(false);
        server.expect(requestTo(URL))
            .andExpect(jsonPath("$.response_format.type").value("json_object"))
            .andExpect(jsonPath("$.reasoning_effort").doesNotExist())
            .andRespond(withSuccess(completion("```json\n{\"intent\":\"STATUS\"}\n```", "stop"), MediaType.APPLICATION_JSON));
        
        assertThat(client.extract(MESSAGES).chatIntent()).isEqualTo(ChatIntent.STATUS);
    }
    
    @Test
    void missingKeyFailsWithoutCallingTheApi() {
        properties.setApiKey("  ");
        assertThatThrownBy(() -> client.extract(MESSAGES))
            .isInstanceOf(AiServiceException.class)
            .extracting(e -> ((AiServiceException) e).getType()).isEqualTo(Type.NOT_CONFIGURED);
        server.verify(); // no request expected
    }
    
    @Test
    void rejectedKeyIsReportedWithoutLeakingIt() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED)
            .body("{\"error\":{\"message\":\"Invalid API Key\"}}").contentType(MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.extract(MESSAGES))
            .isInstanceOf(AiServiceException.class)
            .satisfies(e -> {
                assertThat(((AiServiceException) e).getType()).isEqualTo(Type.AUTHENTICATION);
                assertThat(e.getMessage()).doesNotContain(FAKE_KEY);
            });
    }
    
    @Test
    void quotaExceededIsRateLimited() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        assertType(Type.RATE_LIMITED);
    }
    
    @Test
    void serverErrorIsUnavailable() {
        server.expect(requestTo(URL)).andRespond(withServerError());
        assertType(Type.UNAVAILABLE);
    }
    
    @Test
    void timeoutIsUnavailable() {
        server.expect(requestTo(URL)).andRespond(withException(new SocketTimeoutException("Read timed out")));
        assertType(Type.UNAVAILABLE);
    }
    
    @Test
    void decommissionedModelIsInvalidRequest() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST));
        assertType(Type.INVALID_REQUEST);
    }
    
    @Test
    void nonJsonContentIsInvalidResponse() {
        server.expect(requestTo(URL)).andRespond(withSuccess(
            completion("Bien sûr ! Voici une recette de gâteau...", "stop"), MediaType.APPLICATION_JSON));
        assertType(Type.INVALID_RESPONSE);
    }
    
    @Test
    void truncatedAnswerIsInvalidResponse() {
        server.expect(requestTo(URL)).andRespond(withSuccess(
            completion("{\"intent\":\"CRE", "length"), MediaType.APPLICATION_JSON));
        assertType(Type.INVALID_RESPONSE);
    }
    
    @Test
    void emptyChoicesIsInvalidResponse() {
        server.expect(requestTo(URL)).andRespond(withSuccess("{\"choices\":[]}", MediaType.APPLICATION_JSON));
        assertType(Type.INVALID_RESPONSE);
    }
    
    @Test
    void unknownIntentFromModelBecomesOutOfScope() {
        server.expect(requestTo(URL)).andRespond(withSuccess(
            completion("{\"intent\":\"WRITE_JAVA_CODE\"}", "stop"), MediaType.APPLICATION_JSON));
        assertThat(client.extract(MESSAGES).chatIntent()).isEqualTo(ChatIntent.OUT_OF_SCOPE);
    }
    
    private void assertType(Type expected) {
        assertThatThrownBy(() -> client.extract(MESSAGES))
            .isInstanceOf(AiServiceException.class)
            .extracting(e -> ((AiServiceException) e).getType()).isEqualTo(expected);
    }
}
