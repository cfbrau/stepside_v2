package com.stepside.StepSide.ttos.model; // <-- UNIFICADO: Vive en la feature gobernante de ttos

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Subdocumento embebido BSON para la captura de telemetría en tiempo real.
 * Encapsulado con alta cohesión adentro de la característica 'ttos/model'.
 */
@Getter
@Setter
@NoArgsConstructor
public class PositionInfo {

    @Field("device_id")
    private String deviceId;

    @Field("latitude")
    private Double latitude;

    @Field("longitude")
    private Double longitude;

    @Field("speed")
    private Double speed;

    @Field("heading")
    private Double heading;

    @Field("accuracy")
    private Double accuracy;

    @Field("altitude")
    private Double altitude;

    @Field("battery_level")
    private Integer batteryLevel;

    @Field("eventName")
    private String eventName;

    @Field("address")
    private String address;

    @Field("position_timestamp")
    private Long positionTimestamp;

    @Field("created_at")
    private Long createdAt;
}
