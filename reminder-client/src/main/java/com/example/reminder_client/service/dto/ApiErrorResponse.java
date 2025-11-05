/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.reminder_client.service.dto;

import java.util.Map;

/**
 *
 * @author danil
 */
public record ApiErrorResponse<T>(
        String errorCode,
        String message,
        Map<String, T> details
) {}

