package com.graduation.forumservice.service;

import com.graduation.forumservice.model.DatabaseSequence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import static org.springframework.data.mongodb.core.FindAndModifyOptions.options;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Service
public class SequenceGeneratorService {
    @Autowired
    private MongoOperations mongoOperations;

    public void seedSequence(String seqName, long startValue) {
        // Find by the ID string (e.g., "mongo_content_sequence")
        boolean exists = mongoOperations.exists(query(where("_id").is(seqName)), DatabaseSequence.class);
        if (!exists) {
            DatabaseSequence seed = new DatabaseSequence();
            seed.setId(seqName); // Fix: Use the standard ID setter
            seed.setSeq(startValue - 1);
            mongoOperations.save(seed);
        }
    }

    public int generateSequence(String seqName) {
        DatabaseSequence counter = mongoOperations.findAndModify(
                query(where("_id").is(seqName)),
                new Update().inc("seq", 1),
                options().returnNew(true).upsert(true),
                DatabaseSequence.class);

        // If something goes wrong, return 300 as a safety starting point
        return (int) (counter != null ? counter.getSeq() : 300);
    }
}
