package com.stepside.StepSide.ttos.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Entidad de dominio universal polimórfica para MongoDB Atlas.
 * Saneada por Fabián bloqueando la discriminación del campo _class del pasado.
 */
@Document(collection = "ttos")
@Getter
@Setter
@NoArgsConstructor
public class Tto {

    @Id // Convierte de forma inquebrantable el _id binario de Atlas a String en memoria
    @Field(value = "_id", targetType = FieldType.OBJECT_ID)
    private String id;

    @Indexed(unique = true)
    @Field("code")
    private String code;

    @Field("tto")
    private String ttoName;

    @Indexed
    @Field("tto_type_name")
    private String ttoTypeName;

    @Field("tto_status_name")
    private String ttoStatusName;

    @Field("created_at")
    private Date createdAt = new Date();

    @Field("updated_at")
    private Date updatedAt = new Date();

    @Field("attributes")
    private Map<String, Object> attributes;

    @Field("relations")
    private List<Map<String, Object>> relations = new ArrayList<>();

    @Field("lastPosition")
    private PositionInfo lastPosition;

    @Field("lastHighPriorityPosition")
    private PositionInfo lastHighPriorityPosition;
}
