package com.example.demo.repository;

import com.example.demo.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m JOIN FETCH m.sender WHERE m.chatRoom.id = :chatRoomId ORDER BY m.timestamp ASC")
    List<Message> findByChatRoomIdOrderByTimestampAsc(@Param("chatRoomId") Long chatRoomId);

    @Query(value = "SELECT * FROM hackerton.messages WHERE chat_room_id = :chatRoomId ORDER BY timestamp DESC LIMIT 1", nativeQuery = true)
    Optional<Message> findFirstByChatRoomIdOrderByTimestampDesc(@Param("chatRoomId") Long chatRoomId);

    @Query("SELECT m FROM Message m JOIN FETCH m.sender WHERE m.chatRoom.id = :chatRoomId AND m.timestamp > :since ORDER BY m.timestamp ASC")
    List<Message> findNewMessages(@Param("chatRoomId") Long chatRoomId, @Param("since") LocalDateTime since);
}
