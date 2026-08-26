package org.vgk.hr.position;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Должность")
public record PositionResponse(
        @Schema(description = "Идентификатор") Long id,
        @Schema(description = "Название") String name,
        @Schema(description = "Код") String code,
        @Schema(description = "Активна") boolean active
) {
    public static PositionResponse from(Position position) {
        return new PositionResponse(
                position.getId(),
                position.getName(),
                position.getCode(),
                position.isActive()
        );
    }
}
