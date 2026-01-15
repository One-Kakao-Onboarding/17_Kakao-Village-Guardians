package com.example.demo.repository;

import com.example.demo.entity.MessageRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageReadRepository extends JpaRepository<MessageRead, Long> {

    @Query("SELECT mr FROM MessageRead mr WHERE mr.message.id = :messageId AND mr.user.id = :userId")
    Optional<MessageRead> findByMessageIdAndUserId(@Param("messageId") Long messageId, @Param("userId") Long userId);

    @Query("SELECT mr.message.id FROM MessageRead mr WHERE mr.message.chatRoom.id = :chatRoomId AND mr.user.id = :userId")
    List<Long> findReadMessageIdsByChatRoomAndUser(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);
}
