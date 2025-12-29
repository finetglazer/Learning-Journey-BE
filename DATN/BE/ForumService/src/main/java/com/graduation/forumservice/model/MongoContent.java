package com.graduation.forumservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "mongo_contents")
public class MongoContent {
    @Id
    private ObjectId mongoContentObjId;

    @Field("id")
    @Indexed(unique = true)
    private Integer id;

    @Field("content")
    private Map<String, Object> content;

}
