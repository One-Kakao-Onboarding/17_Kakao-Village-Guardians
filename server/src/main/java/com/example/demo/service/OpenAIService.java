package com.example.demo.service;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OpenAIService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAIService.class);

    private final OpenAIClient client;
    private final String model;

    public OpenAIService(@Value("${openai.api.key}") String apiKey,
                        @Value("${openai.model}") String model) {
        this.model = model;
        this.client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();

        logger.info("OpenAI client initialized with model: {}", model);
    }

    public String convertToPoliteText(String originalText) {
        if (originalText == null || originalText.trim().isEmpty()) {
            return originalText;
        }

        try {
            logger.debug("Calling OpenAI API to convert text: {}", originalText);

            String systemPrompt = "당신은 텍스트를 공손하고 정중한 표현으로 변환하는 전문가입니다. 존댓말을 사용하고 격식있는 표현으로 바꿔주세요.";
            String userPrompt = "다음 텍스트를 매우 공손하고 정중한 표현으로 변환해주세요. 변환된 텍스트만 출력하고 추가 설명은 하지 마세요:\n\n" + originalText;

            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .model(ChatModel.of(model))
                    .addSystemMessage(systemPrompt)
                    .addUserMessage(userPrompt)
                    .temperature(0.7)
                    .maxCompletionTokens(500L)
                    .build();

            ChatCompletion chatCompletion = client.chat().completions().create(params);

            String politeText = chatCompletion.choices().stream()
                    .findFirst()
                    .flatMap(choice -> choice.message().content())
                    .orElse(originalText);

            logger.info("Text converted successfully. Original: '{}', Polite: '{}'", originalText, politeText);

            return politeText;

        } catch (Exception e) {
            logger.error("Failed to convert text to polite version", e);
            return originalText;
        }
    }
}
