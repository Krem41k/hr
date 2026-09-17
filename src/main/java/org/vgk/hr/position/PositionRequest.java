package org.vgk.hr.position;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Создание или обновление должности")
public record PositionRequest(
        @Schema(description = "Название должности", example = "Водитель легкового автомобиля")
        String name,
        @Schema(description = "Уникальный код должности", example = "driver-car")
        String code,
        @Schema(description = "Активна ли должность", example = "true")
        Boolean active
) {
}
