package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.response.EmoticonResponse;
import com.example.demo.service.EmoticonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/emoticons")
public class EmoticonController {

    private static final Logger logger = LoggerFactory.getLogger(EmoticonController.class);
    private final EmoticonService emoticonService;

    @Autowired
    public EmoticonController(EmoticonService emoticonService) {
        this.emoticonService = emoticonService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmoticonResponse>>> getAllEmoticons(
            @RequestParam(required = false) String category) {
        logger.info("GET /api/v1/emoticons - Fetching emoticons" + (category != null ? " for category: " + category : ""));

        List<EmoticonResponse> emoticons;
        if (category != null) {
            emoticons = emoticonService.getEmoticonsByCategory(category);
        } else {
            emoticons = emoticonService.getAllEmoticons();
        }

        return ResponseEntity.ok(ApiResponse.success(emoticons));
    }

    @GetMapping("/{emoticonId}")
    public ResponseEntity<ApiResponse<EmoticonResponse>> getEmoticon(@PathVariable Long emoticonId) {
        logger.info("GET /api/v1/emoticons/{} - Fetching emoticon", emoticonId);
        EmoticonResponse emoticon = emoticonService.getEmoticon(emoticonId);
        return ResponseEntity.ok(ApiResponse.success(emoticon));
    }
}
