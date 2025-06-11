package com.example.FamilyHub.controller;

import com.example.FamilyHub.models.ResponseTo;
import com.example.FamilyHub.service.ExpenseAnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/home/analysis")
public class ExpenseAnalysisController {

    private final ExpenseAnalysisService expenseAnalysisService;

    public ExpenseAnalysisController(ExpenseAnalysisService expenseAnalysisService) {
        this.expenseAnalysisService = expenseAnalysisService;
    }

    @GetMapping("/monthly/{userId}")
    public ResponseEntity<ResponseTo> getMonthlyAnalysis(
            @PathVariable String userId,
            @RequestHeader("Authorization") String authorization) {
        ResponseTo response = expenseAnalysisService.getMonthlyAnalysis(userId, authorization);
        if (response.getError() != null) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }
} 