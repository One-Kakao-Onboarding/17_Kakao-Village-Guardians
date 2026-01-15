package com.example.demo.repository;

import com.example.demo.entity.Emoticon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmoticonRepository extends JpaRepository<Emoticon, Long> {

    List<Emoticon> findByCategory(String category);
}
