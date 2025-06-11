package com.example.FamilyHub.service;

import com.example.FamilyHub.models.ResponseTo;

public interface ExpenseAnalysisService {
    ResponseTo getMonthlyAnalysis(String userId, String authorization);
} 