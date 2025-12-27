package com.graduation.forumservice.repository;

import com.graduation.forumservice.model.MongoContent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface MongoContentRepository extends MongoRepository<MongoContent, Integer> {
    @Query("{ 'id' : ?0 }")
    Optional<MongoContent> findByIntId(int id);
}
