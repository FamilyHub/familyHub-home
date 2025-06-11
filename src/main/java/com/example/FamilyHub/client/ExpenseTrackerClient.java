package com.example.FamilyHub.client;

import com.example.FamilyHub.models.ResponseTo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-expense-tracker", path = "/api/v1/analysis")
public interface ExpenseTrackerClient {
    
    @GetMapping("/monthly/{userId}")
    ResponseEntity<ResponseTo> getMonthlyAnalysis(
            @PathVariable String userId,
            @RequestHeader("Authorization") String authorization);
} 