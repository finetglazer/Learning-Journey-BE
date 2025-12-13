package com.graduation.documentservice.repository;

import com.graduation.documentservice.model.DocContent;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocContentRepository extends MongoRepository<DocContent, ObjectId> {

    Optional<DocContent> findById(String id);

    Optional<DocContent> findByPgNodeId(Long pgNodeId);

    void deleteByPgNodeId(Long pgNodeId);

    boolean existsByPgNodeId(Long pgNodeId);
}
