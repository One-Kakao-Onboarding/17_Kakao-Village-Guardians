package com.example.demo.service;

import com.example.demo.dto.response.EmoticonResponse;
import com.example.demo.entity.Emoticon;
import com.example.demo.exception.EntityNotFoundException;
import com.example.demo.repository.EmoticonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class EmoticonService {

    private static final Logger logger = LoggerFactory.getLogger(EmoticonService.class);
    private final EmoticonRepository emoticonRepository;

    @Autowired
    public EmoticonService(EmoticonRepository emoticonRepository) {
        this.emoticonRepository = emoticonRepository;
    }

    public List<EmoticonResponse> getAllEmoticons() {
        List<Emoticon> emoticons = emoticonRepository.findAll();
        return emoticons.stream()
                .map(this::toEmoticonResponse)
                .collect(Collectors.toList());
    }

    public List<EmoticonResponse> getEmoticonsByCategory(String category) {
        List<Emoticon> emoticons = emoticonRepository.findByCategory(category);
        return emoticons.stream()
                .map(this::toEmoticonResponse)
                .collect(Collectors.toList());
    }

    public EmoticonResponse getEmoticon(Long emoticonId) {
        Emoticon emoticon = emoticonRepository.findById(emoticonId)
                .orElseThrow(() -> new EntityNotFoundException("Emoticon not found with id: " + emoticonId));
        return toEmoticonResponse(emoticon);
    }

    private EmoticonResponse toEmoticonResponse(Emoticon emoticon) {
        return new EmoticonResponse(
                emoticon.getId(),
                emoticon.getName(),
                emoticon.getImageUrl(),
                emoticon.getCategory(),
                emoticon.getCreatedAt()
        );
    }
}
