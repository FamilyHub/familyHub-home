package com.example.FamilyHub.service;

import com.example.FamilyHub.model.ResponseTo;

public interface ExpenseAnalysisService {
    ResponseTo getMonthlyAnalysis(String userId, String authorization);
} 