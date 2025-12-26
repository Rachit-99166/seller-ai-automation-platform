package com.sellerautomation.seller_ai_automation;

import com.sellerautomation.seller_ai_automation.service.GroqService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroqServiceTest {

    private GroqService groqService;

    @Mock
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        groqService = new GroqService();
        // Inject the Mock RestTemplate and @Value properties
        ReflectionTestUtils.setField(groqService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(groqService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(groqService, "apiUrl", "http://fake-api.com");
        ReflectionTestUtils.setField(groqService, "model", "llama3-test");
    }

    // --- TEST 1: VERIFY HEADERS & REQUEST BODY (Happy Path) ---
    @Test
    void testCallGroqAi_CorrectRequestStructure() {
        // Mock a valid response
        mockAiResponse("{\"action\": \"TEST\"}");

        // Call any public method to trigger the private callGroqAi
        groqService.parseUserCommand("test command");

        // Capture the actual request sent to RestTemplate
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(anyString(), entityCaptor.capture(), eq(Map.class));

        HttpEntity<Map<String, Object>> capturedEntity = entityCaptor.getValue();
        Map<String, Object> body = capturedEntity.getBody();

        // ASSERTIONS
        // 1. Check Auth Header
        assertEquals("Bearer test-api-key", capturedEntity.getHeaders().getFirst("Authorization"));
        // 2. Check Model Name
        assertEquals("llama3-test", body.get("model"));
        // 3. Check Message Content
        List<Map<String, String>> messages = (List<Map<String, String>>) body.get("messages");
        assertTrue(messages.get(0).get("content").contains("test command"));
    }

    // --- TEST 2: ERROR HANDLING (API Down) ---
    @Test
    void testCallGroqAi_WhenApiFails_ReturnsEmptyJson() {
        // Force RestTemplate to throw an exception
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("API Connection Refused"));

        // Execute logic
        String result = groqService.parseUserCommand("this will fail");

        // Should catch exception and return fallback JSON
        assertEquals("{}", result); 
    }

    // --- HELPER METHOD ---
    private void mockAiResponse(String content) {
        Map<String, Object> message = new HashMap<>();
        message.put("content", content);
        
        Map<String, Object> choice = new HashMap<>();
        choice.put("message", message);
        
        Map<String, Object> body = new HashMap<>();
        body.put("choices", Collections.singletonList(choice));

        ResponseEntity<Map> response = new ResponseEntity<>(body, HttpStatus.OK);
        
        // Lenient stubbing allows this to work even if arguments aren't 100% exact matches
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);
    }
}