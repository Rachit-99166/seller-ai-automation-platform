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
        ReflectionTestUtils.setField(groqService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(groqService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(groqService, "apiUrl", "http://fake-api.com");
        ReflectionTestUtils.setField(groqService, "model", "llama3-test");
    }

    @Test
    void testCallGroqAi_CorrectRequestStructure() {
        mockAiResponse("{\"action\": \"TEST\"}");

        groqService.parseUserCommand("test command");

        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(anyString(), entityCaptor.capture(), eq(Map.class));

        HttpEntity<Map<String, Object>> capturedEntity = entityCaptor.getValue();
        Map<String, Object> body = capturedEntity.getBody();

        assertEquals("Bearer test-api-key", capturedEntity.getHeaders().getFirst("Authorization"));
        assertEquals("llama3-test", body.get("model"));
        List<Map<String, String>> messages = (List<Map<String, String>>) body.get("messages");
        assertTrue(messages.get(0).get("content").contains("test command"));
    }

    @Test
    void testCallGroqAi_WhenApiFails_ReturnsEmptyJson() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("API Connection Refused"));

        String result = groqService.parseUserCommand("this will fail");

        assertEquals("{}", result); 
    }

    private void mockAiResponse(String content) {
        Map<String, Object> message = new HashMap<>();
        message.put("content", content);
        
        Map<String, Object> choice = new HashMap<>();
        choice.put("message", message);
        
        Map<String, Object> body = new HashMap<>();
        body.put("choices", Collections.singletonList(choice));

        ResponseEntity<Map> response = new ResponseEntity<>(body, HttpStatus.OK);
        
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);
    }
}