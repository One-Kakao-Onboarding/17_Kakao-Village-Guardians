package com.example.demo.repository;

import com.example.demo.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("SELECT DISTINCT cr FROM ChatRoom cr " +
           "JOIN ChatRoomMember crm ON cr.id = crm.chatRoom.id " +
           "WHERE crm.user.id = :userId")
    List<ChatRoom> findByUserId(@Param("userId") Long userId);
}
