package com.example.FamilyHub.service.impl;

import com.example.FamilyHub.client.ExpenseTrackerClient;
import com.example.FamilyHub.model.ResponseTo;
import com.example.FamilyHub.service.ExpenseAnalysisService;
import org.springframework.stereotype.Service;

@Service
public class ExpenseAnalysisServiceImpl implements ExpenseAnalysisService {

    private final ExpenseTrackerClient expenseTrackerClient;

    public ExpenseAnalysisServiceImpl(ExpenseTrackerClient expenseTrackerClient) {
        this.expenseTrackerClient = expenseTrackerClient;
    }

    @Override
    public ResponseTo getMonthlyAnalysis(String userId, String authorization) {
        return expenseTrackerClient.getMonthlyAnalysis(userId, authorization).getBody();
    }
} 