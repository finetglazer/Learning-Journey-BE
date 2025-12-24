package com.graduation.forumservice.repository;

import com.graduation.forumservice.model.MongoContent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MongoContentRepository extends MongoRepository<MongoContent, Object> {
}
