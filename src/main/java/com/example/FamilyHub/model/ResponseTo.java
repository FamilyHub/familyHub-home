package com.example.FamilyHub.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseTo {
    private List<Double> totalCashIn;
    private List<Double> totalCashOut;
    private String error;
    private String userId;
} 