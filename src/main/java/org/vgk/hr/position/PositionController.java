package org.vgk.hr.position;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/positions")
@RequiredArgsConstructor
@Tag(name = "Positions", description = "CRUD должностей и form-schema")
public class PositionController {

    private final PositionService positionService;

    @Operation(summary = "Создать должность")
    @PostMapping
    public ResponseEntity<PositionResponse> create(@RequestBody PositionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(positionService.create(request));
    }

    @Operation(summary = "Список должностей")
    @GetMapping
    public List<PositionResponse> list() {
        return positionService.list();
    }

    @Operation(summary = "Получить должность")
    @GetMapping("/{id}")
    public PositionResponse get(@PathVariable Long id) {
        return positionService.get(id);
    }

    @Operation(summary = "Обновить должность")
    @PutMapping("/{id}")
    public PositionResponse update(@PathVariable Long id, @RequestBody PositionRequest request) {
        return positionService.update(id, request);
    }

    @Operation(summary = "Деактивировать должность")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        positionService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Схема формы оператора: уникальные USER-поля активных PDF")
    @GetMapping("/{id}/form-schema")
    public FormSchemaResponse formSchema(@PathVariable Long id) {
        return positionService.formSchema(id);
    }
}
