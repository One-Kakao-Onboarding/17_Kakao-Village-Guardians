package com.example.demo.service;

import com.example.demo.dto.response.*;
import com.example.demo.entity.ChatRoom;
import com.example.demo.entity.Message;
import com.example.demo.entity.Profile;
import com.example.demo.entity.User;
import com.example.demo.repository.ChatRoomRepository;
import com.example.demo.repository.ProfileRepository;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AIService {

    private static final Logger logger = LoggerFactory.getLogger(AIService.class);

    private final OpenAIClient client;
    private final String model;
    private final ChatRoomRepository chatRoomRepository;
    private final ProfileRepository profileRepository;
    private final UserService userService;

    @Autowired
    public AIService(@Value("${openai.api.key}") String apiKey,
                     @Value("${openai.model}") String model,
                     ChatRoomRepository chatRoomRepository,
                     ProfileRepository profileRepository,
                     UserService userService) {
        this.model = model;
        this.client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
        this.chatRoomRepository = chatRoomRepository;
        this.profileRepository = profileRepository;
        this.userService = userService;

        logger.info("AI Service initialized with model: {}", model);
    }

    public TransformTextResponse transformText(String text, Double formalityLevel, String relationship, String personaId) {
        TransformTextResponse response = new TransformTextResponse();
        response.setOriginalText(text);
        response.setFormalityLevel(formalityLevel);

        // Determine persona based on formality level
        String appliedPersona = personaId != null ? personaId : determinePersona(formalityLevel);
        response.setAppliedPersona(appliedPersona);

        if (text == null || text.trim().isEmpty()) {
            response.setTransformedText(text);
            response.setChanges(new ArrayList<>());
            response.setShouldSuggest(false);
            return response;
        }

        try {
            logger.debug("Transforming text with formality: {}, persona: {}, relationship: {}", formalityLevel, appliedPersona, relationship);

            String systemPrompt = "당신은 텍스트를 다양한 격식 수준과 관계에 맞게 변환하는 전문가입니다.";

            // Persona별 가이드라인
            String personaGuide = getPersonaGuide(appliedPersona);

            String userPrompt = String.format(
                "다음 텍스트를 '%s' 말투로 변환해주세요.\n\n" +
                "**말투 가이드:**\n%s\n\n" +
                "**변환 규칙:**\n" +
                "- 원본 텍스트의 의미는 그대로 유지\n" +
                "- 말투와 문체만 변경\n" +
                "- 변환된 텍스트만 출력 (설명 없이)\n\n" +
                "**원본 텍스트:**\n%s",
                appliedPersona, personaGuide, text
            );

            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .model(ChatModel.of(model))
                    .addSystemMessage(systemPrompt)
                    .addUserMessage(userPrompt)
                    .temperature(0.7)
                    .maxCompletionTokens(500L)
                    .build();

            ChatCompletion chatCompletion = client.chat().completions().create(params);

            String transformedText = chatCompletion.choices().stream()
                    .findFirst()
                    .flatMap(choice -> choice.message().content())
                    .orElse(text);

            response.setTransformedText(transformedText);

            // Analyze changes
            List<TransformTextResponse.ChangeDetail> changes = analyzeChanges(text, transformedText, formalityLevel);
            response.setChanges(changes);

            // Determine if suggestion should be shown
            boolean shouldSuggest = !text.equals(transformedText) && (formalityLevel >= 60 || "boss".equals(relationship) || "senior".equals(relationship));
            response.setShouldSuggest(shouldSuggest);

            if (shouldSuggest) {
                response.setSuggestionReason(getSuggestionReason(relationship, Double.toString(formalityLevel)));
            }

            logger.info("Text transformed successfully");
            return response;

        } catch (Exception e) {
            logger.error("Failed to transform text", e);
            response.setTransformedText(text);
            response.setChanges(new ArrayList<>());
            response.setShouldSuggest(false);
            return response;
        }
    }

    public EmotionGuardDetailResponse checkEmotionGuard(String text, String personaId) {
        if (text == null || text.trim().isEmpty()) {
            return new EmotionGuardDetailResponse(false, null, 0.0, null, null);
        }

        try {
            // persona가 없으면 기본값 사용
            String appliedPersona = personaId != null ? personaId : "casual-polite";
            String personaGuide = getPersonaGuide(appliedPersona);

            logger.debug("Checking emotion guard for text with persona: {}", appliedPersona);

            String systemPrompt = "당신은 텍스트의 감정을 분석하는 전문가입니다. 공격적이거나 비꼬는 표현을 감지합니다.";
            String userPrompt = String.format(
                "다음 텍스트를 분석하여 JSON 형식으로 응답해주세요.\n\n" +
                "**말투 설정:** %s\n" +
                "%s\n\n" +
                "{\n" +
                "  \"isAggressive\": true/false,\n" +
                "  \"aggressionType\": \"sarcasm|passive_aggressive|direct_attack|dismissive\",\n" +
                "  \"aggressionScore\": 0.0-1.0,\n" +
                "  \"suggestion\": \"위 말투에 맞는 더 나은 표현 제안\"\n" +
                "}\n\n" +
                "**중요:** suggestion은 반드시 위의 말투 가이드를 정확히 따라야 합니다!\n\n" +
                "텍스트: %s", appliedPersona, personaGuide, text
            );

            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .model(ChatModel.of(model))
                    .addSystemMessage(systemPrompt)
                    .addUserMessage(userPrompt)
                    .temperature(0.5)
                    .maxCompletionTokens(300L)
                    .build();

            ChatCompletion chatCompletion = client.chat().completions().create(params);

            String aiResponse = chatCompletion.choices().stream()
                    .findFirst()
                    .flatMap(choice -> choice.message().content())
                    .orElse("{}");

            boolean isAggressive = aiResponse.contains("\"isAggressive\": true");
            String aggressionType = extractField(aiResponse, "aggressionType");
            double aggressionScore = extractScore(aiResponse);
            String suggestion = extractField(aiResponse, "suggestion");

            String warningMessage = isAggressive ? "조금 더 부드럽게 말해볼까요?" : null;

            return new EmotionGuardDetailResponse(isAggressive, aggressionType, aggressionScore, suggestion, warningMessage);

        } catch (Exception e) {
            logger.error("Failed to check emotion guard", e);
            return new EmotionGuardDetailResponse(false, null, 0.0, null, null);
        }
    }

    public ReactionSuggestResponse suggestReactions(String messageContent, String relationship, Double formalityLevel,
                                                     String personaId, List<Message> conversationHistory, User currentUser) {
        ReactionSuggestResponse response = new ReactionSuggestResponse();

        if (messageContent == null || messageContent.trim().isEmpty()) {
            response.setEmotion("neutral");
            response.setEmotionScore(0.5);
            response.setSuggestedEmojis(Arrays.asList("👍", "❤️", "😊"));
            response.setSuggestedTexts(new ArrayList<>());
            response.setQuickResponses(new ArrayList<>());
            return response;
        }

        try {
            Double formality = formalityLevel != null ? formalityLevel : 50.0;
            String persona = personaId != null ? personaId : determinePersona(formality);
            String personaGuide = getPersonaGuide(persona);

            logger.debug("Suggesting reactions for message with persona: {}, history size: {}",
                        persona, conversationHistory != null ? conversationHistory.size() : 0);

            // 대화 히스토리 구성
            StringBuilder historyContext = new StringBuilder();
            if (conversationHistory != null && !conversationHistory.isEmpty()) {
                historyContext.append("**대화 맥락 (최근 대화):**\n");
                int messageCount = 0;
                for (Message msg : conversationHistory) {
                    String senderLabel = currentUser != null && msg.getSender().getId().equals(currentUser.getId()) ? "나" : "상대방";
                    historyContext.append(String.format("%s: %s\n", senderLabel, msg.getContent()));
                    messageCount++;
                    if (messageCount >= 20) { // 프롬프트에는 최근 20개만 포함
                        historyContext.append("... (이전 대화 생략)\n");
                        break;
                    }
                }
                historyContext.append("\n");
            }

            String systemPrompt = "당신은 메시지 감정을 분석하고 적절한 반응을 추천하는 전문가입니다. 대화의 맥락을 고려하여 자연스럽고 적절한 반응을 제안합니다.";
            String userPrompt = String.format(
                "다음 메시지를 분석하여 JSON 형식으로 응답해주세요.\n\n" +
                "%s" +
                "**말투 설정:** %s\n" +
                "%s\n\n" +
                "{\n" +
                "  \"emotion\": \"happy|sad|angry|surprised|excited|worried|neutral\",\n" +
                "  \"emotionScore\": 0.0-1.0,\n" +
                "  \"suggestedEmojis\": [\"😊\", \"❤️\", ...] (5개),\n" +
                "  \"suggestedTexts\": [\n" +
                "    {\"text\": \"위 말투에 정확히 맞고 대화 맥락을 반영한 답장 텍스트\", \"type\": \"comfort|empathy|question|support\"},\n" +
                "    ...\n" +
                "  ] (위 말투 가이드를 정확히 따라서 2-3개),\n" +
                "  \"quickResponses\": [\n" +
                "    {\"text\": \"위 말투에 정확히 맞고 대화 맥락을 반영한 빠른 답장\", \"icon\": \"😊\"},\n" +
                "    ...\n" +
                "  ] (위 말투 가이드를 정확히 따라서 2-3개)\n" +
                "}\n\n" +
                "**중요:** \n" +
                "- suggestedTexts와 quickResponses의 text는 반드시 위의 말투 가이드를 정확히 따라야 합니다!\n" +
                "- 대화 맥락을 고려하여 자연스럽고 상황에 맞는 응답을 생성하세요.\n\n" +
                "**현재 메시지:** %s",
                historyContext.toString(), persona, personaGuide, messageContent
            );

            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .model(ChatModel.of(model))
                    .addSystemMessage(systemPrompt)
                    .addUserMessage(userPrompt)
                    .temperature(0.7)
                    .maxCompletionTokens(500L)
                    .build();

            ChatCompletion chatCompletion = client.chat().completions().create(params);

            String aiResponse = chatCompletion.choices().stream()
                    .findFirst()
                    .flatMap(choice -> choice.message().content())
                    .orElse("{}");

            // Parse AI response
            response.setEmotion(extractField(aiResponse, "emotion"));
            response.setEmotionScore(extractEmotionScore(aiResponse));
            response.setSuggestedEmojis(extractEmojis(aiResponse));
            response.setSuggestedTexts(extractSuggestedTexts(aiResponse));
            response.setQuickResponses(extractQuickResponses(aiResponse));

            logger.info("Generated reaction suggestions via OpenAI for emotion: {}", response.getEmotion());
            return response;

        } catch (Exception e) {
            logger.error("Failed to suggest reactions via OpenAI", e);
            response.setEmotion("neutral");
            response.setEmotionScore(0.5);
            response.setSuggestedEmojis(Arrays.asList("👍", "❤️", "😊", "🙌", "✅"));
            response.setSuggestedTexts(new ArrayList<>());
            response.setQuickResponses(new ArrayList<>());
            return response;
        }
    }

    public FriendMatchingDetailResponse findFriendMatches(String profileName, String personaId, List<Long> chatRoomIds) {
        try {
            logger.debug("Finding friend matches for profile: {}", profileName);

            List<FriendMatchingDetailResponse.ChatRoomRecommendation> recommendations = new ArrayList<>();

            if (chatRoomIds != null && !chatRoomIds.isEmpty()) {
                // Build chat room info for AI analysis
                StringBuilder chatRoomInfo = new StringBuilder();
                List<ChatRoom> chatRooms = new ArrayList<>();

                for (Long chatRoomId : chatRoomIds) {
                    chatRoomRepository.findById(chatRoomId).ifPresent(chatRoom -> {
                        chatRooms.add(chatRoom);
                        chatRoomInfo.append(String.format(
                            "- ID: %d, 이름: %s, 격식도: %s, 관계: %s\n",
                            chatRoom.getId(),
                            chatRoom.getName(),
                            chatRoom.getFormalityLevel() != null ? chatRoom.getFormalityLevel() : "미설정",
                            chatRoom.getRelationship() != null ? chatRoom.getRelationship() : "미설정"
                        ));
                    });
                }

                if (!chatRooms.isEmpty()) {
                    String systemPrompt = "당신은 사용자의 프로필과 채팅방을 분석하여 가장 적합한 채팅방을 추천하는 전문가입니다.";
                    String userPrompt = String.format(
                        "다음 프로필에 가장 적합한 채팅방들을 분석하여 JSON 배열로 응답해주세요:\n\n" +
                        "프로필: %s (페르소나: %s)\n\n" +
                        "채팅방 목록:\n%s\n" +
                        "JSON 형식:\n" +
                        "[\n" +
                        "  {\n" +
                        "    \"chatRoomId\": 1,\n" +
                        "    \"matchScore\": 85,\n" +
                        "    \"matchReason\": \"이유 설명\"\n" +
                        "  },\n" +
                        "  ...\n" +
                        "]\n\n" +
                        "matchScore는 0-100 사이 값으로, 프로필의 페르소나와 채팅방의 격식도/관계가 얼마나 잘 맞는지 평가해주세요.",
                        profileName, personaId != null ? personaId : "미설정", chatRoomInfo.toString()
                    );

                    ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                            .model(ChatModel.of(model))
                            .addSystemMessage(systemPrompt)
                            .addUserMessage(userPrompt)
                            .temperature(0.5)
                            .maxCompletionTokens(700L)
                            .build();

                    ChatCompletion chatCompletion = client.chat().completions().create(params);

                    String aiResponse = chatCompletion.choices().stream()
                            .findFirst()
                            .flatMap(choice -> choice.message().content())
                            .orElse("[]");

                    // Parse AI recommendations
                    recommendations = parseMatchRecommendations(aiResponse, chatRooms);

                    logger.info("Generated {} recommendations via OpenAI", recommendations.size());
                }
            }

            // Sort by match score
            recommendations.sort((a, b) -> b.getMatchScore().compareTo(a.getMatchScore()));

            return new FriendMatchingDetailResponse(recommendations);

        } catch (Exception e) {
            logger.error("Failed to find friend matches via OpenAI", e);
            // Fallback to simple matching
            return fallbackFriendMatching(profileName, personaId, chatRoomIds);
        }
    }

    private FriendMatchingDetailResponse fallbackFriendMatching(String profileName, String personaId, List<Long> chatRoomIds) {
        List<FriendMatchingDetailResponse.ChatRoomRecommendation> recommendations = new ArrayList<>();

        if (chatRoomIds != null && !chatRoomIds.isEmpty()) {
            for (Long chatRoomId : chatRoomIds) {
                chatRoomRepository.findById(chatRoomId).ifPresent(chatRoom -> {
                    int matchScore = calculateMatchScore(profileName, personaId, chatRoom);
                    String matchReason = generateMatchReason(profileName, personaId, chatRoom);

                    recommendations.add(new FriendMatchingDetailResponse.ChatRoomRecommendation(
                            chatRoom.getId(),
                            chatRoom.getName(),
                            matchScore,
                            matchReason
                    ));
                });
            }
            recommendations.sort((a, b) -> b.getMatchScore().compareTo(a.getMatchScore()));
        }

        return new FriendMatchingDetailResponse(recommendations);
    }

    private List<FriendMatchingDetailResponse.ChatRoomRecommendation> parseMatchRecommendations(String aiResponse, List<ChatRoom> chatRooms) {
        List<FriendMatchingDetailResponse.ChatRoomRecommendation> recommendations = new ArrayList<>();

        try {
            // Find all JSON objects in the array
            String[] objects = aiResponse.split("\\{");
            for (String obj : objects) {
                if (obj.contains("\"chatRoomId\"") && obj.contains("\"matchScore\"")) {
                    Long chatRoomId = extractLongValue(obj, "chatRoomId");
                    Integer matchScore = extractIntValue(obj, "matchScore");
                    String matchReason = extractJsonValue(obj, "matchReason");

                    if (chatRoomId != null && matchScore != null) {
                        // Find chat room name
                        String chatRoomName = chatRooms.stream()
                                .filter(cr -> cr.getId().equals(chatRoomId))
                                .map(ChatRoom::getName)
                                .findFirst()
                                .orElse("Unknown");

                        recommendations.add(new FriendMatchingDetailResponse.ChatRoomRecommendation(
                                chatRoomId,
                                chatRoomName,
                                matchScore,
                                matchReason != null ? matchReason : "프로필과 잘 맞습니다"
                        ));
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Could not parse match recommendations from AI response");
        }

        return recommendations;
    }

    private Long extractLongValue(String json, String key) {
        try {
            String searchKey = "\"" + key + "\"";
            int keyIndex = json.indexOf(searchKey);
            if (keyIndex != -1) {
                int colonIndex = json.indexOf(":", keyIndex);
                int valueStart = colonIndex + 1;
                int valueEnd = json.indexOf(",", valueStart);
                if (valueEnd == -1) valueEnd = json.indexOf("}", valueStart);
                if (valueStart > colonIndex && valueEnd > valueStart) {
                    String valueStr = json.substring(valueStart, valueEnd).trim();
                    return Long.parseLong(valueStr);
                }
            }
        } catch (Exception e) {
            logger.debug("Could not extract long {} from JSON", key);
        }
        return null;
    }

    private Integer extractIntValue(String json, String key) {
        try {
            String searchKey = "\"" + key + "\"";
            int keyIndex = json.indexOf(searchKey);
            if (keyIndex != -1) {
                int colonIndex = json.indexOf(":", keyIndex);
                int valueStart = colonIndex + 1;
                int valueEnd = json.indexOf(",", valueStart);
                if (valueEnd == -1) valueEnd = json.indexOf("}", valueStart);
                if (valueStart > colonIndex && valueEnd > valueStart) {
                    String valueStr = json.substring(valueStart, valueEnd).trim();
                    return Integer.parseInt(valueStr);
                }
            }
        } catch (Exception e) {
            logger.debug("Could not extract int {} from JSON", key);
        }
        return null;
    }

    // Helper methods
    private String determinePersona(Double formalityLevel) {
        if (formalityLevel >= 80.0) return "very-formal";
        if (formalityLevel >= 60.0) return "formal";
        if (formalityLevel >= 40.0) return "casual-polite";
        if (formalityLevel >= 20.0) return "casual";
        return "very-casual";
    }

    private String getPersonaGuide(String persona) {
        switch (persona) {
            case "very-formal":
                return "- 매우 격식있는 존댓말 사용 (예: ~하십니다, ~입니까, 말씀드리겠습니다)\n" +
                       "- 상사, 고객, 높은 분에게 쓰는 공손한 말투\n" +
                       "- 겸손하고 정중한 표현";

            case "formal":
                return "- 격식있는 존댓말 사용 (예: ~합니다, ~입니다, 확인했습니다)\n" +
                       "- 업무 상황이나 공식적인 자리에서의 존댓말\n" +
                       "- 정중하면서도 명확한 표현";

            case "casual-polite":
                return "- 친근하지만 예의있는 존댓말 (예: ~해요, ~이에요, 좋아요)\n" +
                       "- 선배, 동료에게 쓰는 편한 존댓말\n" +
                       "- 부드럽고 친근한 표현";

            case "casual":
                return "- 친근한 반말 사용 (예: ~해, ~야, 그래, 알았어)\n" +
                       "- 친구, 동료와의 편한 대화\n" +
                       "- 자연스럽고 친밀한 표현";

            case "very-casual":
                return "- 매우 친근하고 편한 반말 (예: ㅇㅇ, ㅋㅋ, ㅇㅋ, 알겠)\n" +
                       "- 친한 친구, 가까운 사이에서 쓰는 말투\n" +
                       "- 축약어, 짧은 표현, 이모티콘 느낌 사용 (ㄱㅅ, ㄱㄱ 등)";

            default:
                return "- 상황에 맞는 적절한 말투";
        }
    }

    private List<TransformTextResponse.ChangeDetail> analyzeChanges(String original, String transformed, Double formalityLevel) {
        List<TransformTextResponse.ChangeDetail> changes = new ArrayList<>();

        if (!original.equals(transformed)) {
            if (formalityLevel >= 60.0) {
                changes.add(new TransformTextResponse.ChangeDetail("tone", "반말을 정중한 존댓말로 변경"));
                if (transformed.length() > original.length() * 1.5) {
                    changes.add(new TransformTextResponse.ChangeDetail("detail", "구체적인 응답으로 확장"));
                }
            } else if (formalityLevel <= 30.0) {
                changes.add(new TransformTextResponse.ChangeDetail("tone", "격식을 낮추고 친근하게 변경"));
            }
        }

        return changes;
    }

    private String getSuggestionReason(String relationship, String formalityLevel) {
        if ("boss".equals(relationship)) {
            return "상사와의 대화에서 더 격식있는 표현이 적합합니다.";
        } else if ("senior".equals(relationship)) {
            return "선배와의 대화에서 예의있는 표현을 사용하는 것이 좋습니다.";
        } else {
            try {
                double level = Double.parseDouble(formalityLevel);
                if (level >= 80.0) {
                    return "업무 상황에서 더 정중한 표현이 적절합니다.";
                }
            } catch (NumberFormatException e) {
                logger.debug("Could not parse formalityLevel: {}", formalityLevel);
            }
        }
        return "상황에 맞는 적절한 표현을 사용해보세요.";
    }

    private String analyzeEmotion(String text) {
        String lowerText = text.toLowerCase();
        if (lowerText.contains("ㅠ") || lowerText.contains("슬프") || lowerText.contains("떨어졌")) return "sad";
        if (lowerText.contains("ㅋ") || lowerText.contains("좋") || lowerText.contains("기쁘")) return "happy";
        if (lowerText.contains("화") || lowerText.contains("짜증")) return "angry";
        if (lowerText.contains("?") || lowerText.contains("놀라")) return "surprised";
        return "neutral";
    }

    private List<String> suggestEmojis(String text, String emotion) {
        switch (emotion) {
            case "sad": return Arrays.asList("😢", "🫂", "💪", "❤️", "🥺");
            case "happy": return Arrays.asList("😊", "🎉", "👏", "💯", "✨");
            case "angry": return Arrays.asList("😰", "🙏", "💙", "🤝", "☕");
            case "surprised": return Arrays.asList("😮", "👀", "😱", "🤯", "❗");
            default: return Arrays.asList("👍", "❤️", "😊", "🙌", "✅");
        }
    }

    private List<ReactionSuggestResponse.SuggestedText> generateSuggestedTexts(String emotion, int formalityLevel) {
        List<ReactionSuggestResponse.SuggestedText> texts = new ArrayList<>();

        if ("sad".equals(emotion)) {
            if (formalityLevel < 40) {
                texts.add(new ReactionSuggestResponse.SuggestedText("괜찮아, 다음에 잘하면 돼!", "comfort"));
                texts.add(new ReactionSuggestResponse.SuggestedText("헐 ㅠㅠ 힘내...", "empathy"));
            } else {
                texts.add(new ReactionSuggestResponse.SuggestedText("힘내세요. 다음에는 더 좋은 결과 있을 거예요.", "comfort"));
                texts.add(new ReactionSuggestResponse.SuggestedText("어려우셨겠어요. 응원하겠습니다.", "empathy"));
            }
        }

        return texts;
    }

    private List<ReactionSuggestResponse.QuickResponse> generateQuickResponses(String emotion) {
        List<ReactionSuggestResponse.QuickResponse> responses = new ArrayList<>();

        if ("sad".equals(emotion)) {
            responses.add(new ReactionSuggestResponse.QuickResponse("무슨 일이야?", "❓"));
            responses.add(new ReactionSuggestResponse.QuickResponse("힘내! 응원할게", "💪"));
        } else if ("happy".equals(emotion)) {
            responses.add(new ReactionSuggestResponse.QuickResponse("축하해!", "🎉"));
            responses.add(new ReactionSuggestResponse.QuickResponse("대박!", "😆"));
        }

        return responses;
    }

    private int calculateMatchScore(String profileName, String personaId, ChatRoom chatRoom) {
        int score = 50; // Base score

        // Match persona with formality
        if (personaId != null && chatRoom.getFormalityLevel() != null) {
            if (personaId.contains("formal") && chatRoom.getFormalityLevel().contains("formal")) {
                score += 30;
            } else if (personaId.contains("casual") && chatRoom.getFormalityLevel().contains("casual")) {
                score += 30;
            }
        }

        // Match relationship
        if (chatRoom.getRelationship() != null) {
            if ("boss".equals(chatRoom.getRelationship()) || "senior".equals(chatRoom.getRelationship())) {
                if (personaId != null && personaId.contains("formal")) {
                    score += 20;
                }
            }
        }

        return Math.min(score, 100);
    }

    private String generateMatchReason(String profileName, String personaId, ChatRoom chatRoom) {
        if (chatRoom.getRelationship() != null) {
            if ("boss".equals(chatRoom.getRelationship())) {
                return "회사/업무 관련 프로필, 상사 관계";
            } else if ("senior".equals(chatRoom.getRelationship())) {
                return "격식있는 말투, 선배 관계";
            } else if ("colleague".equals(chatRoom.getRelationship())) {
                return "업무 동료 관계";
            }
        }

        if (personaId != null && personaId.contains("formal")) {
            return "격식있는 프로필에 적합";
        }

        return "프로필 성향과 잘 맞음";
    }

    private String extractField(String jsonResponse, String fieldName) {
        try {
            int start = jsonResponse.indexOf("\"" + fieldName + "\": \"") + fieldName.length() + 5;
            int end = jsonResponse.indexOf("\"", start);
            if (start > fieldName.length() + 4 && end > start) {
                return jsonResponse.substring(start, end);
            }
        } catch (Exception e) {
            logger.debug("Could not extract {} from response", fieldName);
        }
        return null;
    }

    private double extractScore(String jsonResponse) {
        try {
            int start = jsonResponse.indexOf("\"aggressionScore\": ") + 19;
            int end = jsonResponse.indexOf(",", start);
            if (end == -1) end = jsonResponse.indexOf("}", start);
            if (start > 18 && end > start) {
                String scoreStr = jsonResponse.substring(start, end).trim();
                return Double.parseDouble(scoreStr);
            }
        } catch (Exception e) {
            logger.debug("Could not extract score from response");
        }
        return 0.0;
    }

    private double extractEmotionScore(String jsonResponse) {
        try {
            int start = jsonResponse.indexOf("\"emotionScore\": ") + 16;
            int end = jsonResponse.indexOf(",", start);
            if (end == -1) end = jsonResponse.indexOf("}", start);
            if (start > 15 && end > start) {
                String scoreStr = jsonResponse.substring(start, end).trim();
                return Double.parseDouble(scoreStr);
            }
        } catch (Exception e) {
            logger.debug("Could not extract emotion score from response");
        }
        return 0.5;
    }

    private List<String> extractEmojis(String jsonResponse) {
        List<String> emojis = new ArrayList<>();
        try {
            int start = jsonResponse.indexOf("\"suggestedEmojis\": [") + 20;
            int end = jsonResponse.indexOf("]", start);
            if (start > 19 && end > start) {
                String emojisStr = jsonResponse.substring(start, end);
                String[] emojiArray = emojisStr.split(",");
                for (String emoji : emojiArray) {
                    String cleaned = emoji.trim().replaceAll("\"", "");
                    if (!cleaned.isEmpty()) {
                        emojis.add(cleaned);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Could not extract emojis from response");
        }
        if (emojis.isEmpty()) {
            return Arrays.asList("👍", "❤️", "😊", "🙌", "✅");
        }
        return emojis;
    }

    private List<ReactionSuggestResponse.SuggestedText> extractSuggestedTexts(String jsonResponse) {
        List<ReactionSuggestResponse.SuggestedText> texts = new ArrayList<>();
        try {
            int start = jsonResponse.indexOf("\"suggestedTexts\": [");
            if (start != -1) {
                start += 19;
                int end = jsonResponse.indexOf("]", start);
                if (end > start) {
                    String textsStr = jsonResponse.substring(start, end);
                    // Simple parsing - look for text and type pairs
                    String[] parts = textsStr.split("\\{");
                    for (String part : parts) {
                        if (part.contains("\"text\"") && part.contains("\"type\"")) {
                            String text = extractJsonValue(part, "text");
                            String type = extractJsonValue(part, "type");
                            if (text != null && type != null) {
                                texts.add(new ReactionSuggestResponse.SuggestedText(text, type));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Could not extract suggested texts from response");
        }
        return texts;
    }

    private List<ReactionSuggestResponse.QuickResponse> extractQuickResponses(String jsonResponse) {
        List<ReactionSuggestResponse.QuickResponse> responses = new ArrayList<>();
        try {
            int start = jsonResponse.indexOf("\"quickResponses\": [");
            if (start != -1) {
                start += 19;
                int end = jsonResponse.indexOf("]", start);
                if (end > start) {
                    String responsesStr = jsonResponse.substring(start, end);
                    // Simple parsing - look for text and icon pairs
                    String[] parts = responsesStr.split("\\{");
                    for (String part : parts) {
                        if (part.contains("\"text\"") && part.contains("\"icon\"")) {
                            String text = extractJsonValue(part, "text");
                            String icon = extractJsonValue(part, "icon");
                            if (text != null && icon != null) {
                                responses.add(new ReactionSuggestResponse.QuickResponse(text, icon));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Could not extract quick responses from response");
        }
        return responses;
    }

    private String extractJsonValue(String json, String key) {
        try {
            String searchKey = "\"" + key + "\"";
            int keyIndex = json.indexOf(searchKey);
            if (keyIndex != -1) {
                int colonIndex = json.indexOf(":", keyIndex);
                int valueStart = json.indexOf("\"", colonIndex) + 1;
                int valueEnd = json.indexOf("\"", valueStart);
                if (valueStart > colonIndex && valueEnd > valueStart) {
                    return json.substring(valueStart, valueEnd);
                }
            }
        } catch (Exception e) {
            logger.debug("Could not extract {} from JSON", key);
        }
        return null;
    }

    // Legacy methods for backward compatibility
    public String transformText(String text, String formalityLevel, String relationship) {
        try {
            Double level = Double.parseDouble(formalityLevel);
            TransformTextResponse response = transformText(text, level, relationship, null);
            return response.getTransformedText();
        } catch (Exception e) {
            return text;
        }
    }

    public List<String> suggestReactions(String messageContent, String senderRelationship) {
        ReactionSuggestResponse response = suggestReactions(messageContent, senderRelationship, 50.0, null, new ArrayList<>(), null);
        return response.getSuggestedEmojis();
    }
}
