package com.esprit.reservation.controller;

import com.esprit.reservation.dto.TableResponse;
import com.esprit.reservation.entity.RestaurantTable;
import com.esprit.reservation.entity.TableStatus;
import com.esprit.reservation.mapper.TableMapper;
import com.esprit.reservation.service.TableService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables")
@RequiredArgsConstructor
public class TableController {

    private final TableService tableService;
    private final TableMapper tableMapper;

    @GetMapping
    public ResponseEntity<List<TableResponse>> getTables() {
        return ResponseEntity.ok(tableMapper.toResponseList(tableService.getAllTables()));
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<TableResponse>> getTablesByRoom(@PathVariable Long roomId) {
        return ResponseEntity.ok(tableMapper.toResponseList(tableService.getTablesByRoom(roomId)));
    }

    @PostMapping
    public ResponseEntity<TableResponse> createTable(@RequestBody RestaurantTable table) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tableMapper.toResponse(tableService.createTable(table)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TableResponse> updateTableStatus(@PathVariable Long id, @RequestParam TableStatus status) {
        return ResponseEntity.ok(tableMapper.toResponse(tableService.updateTableStatus(id, status)));
    }
}
