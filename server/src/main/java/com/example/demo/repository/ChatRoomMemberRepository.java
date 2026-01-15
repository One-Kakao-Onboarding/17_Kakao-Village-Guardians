package com.example.demo.repository;

import com.example.demo.entity.ChatRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

    Optional<ChatRoomMember> findByChatRoomIdAndUserId(Long chatRoomId, Long userId);

    @Query("SELECT crm FROM ChatRoomMember crm JOIN FETCH crm.user WHERE crm.chatRoom.id = :chatRoomId")
    List<ChatRoomMember> findByChatRoomId(@Param("chatRoomId") Long chatRoomId);

    List<ChatRoomMember> findByUserId(Long userId);

    @Query("SELECT COUNT(m) FROM Message m " +
           "WHERE m.chatRoom.id = :chatRoomId " +
           "AND m.id > COALESCE(:lastReadMessageId, 0) " +
           "AND m.sender.id != :currentUserId")
    Long countUnreadMessages(
        @Param("chatRoomId") Long chatRoomId,
        @Param("lastReadMessageId") Long lastReadMessageId,
        @Param("currentUserId") Long currentUserId
    );
}
