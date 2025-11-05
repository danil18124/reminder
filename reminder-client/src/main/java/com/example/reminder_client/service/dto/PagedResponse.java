/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.reminder_client.service.dto;

import java.util.List;

/**
 *
 * @author danil
 */
public record PagedResponse<T>(
        List<T> content,
        int totalPages,
        long totalElements,
        int number,
        int size,
        boolean first,
        boolean last,
        int numberOfElements,
        boolean empty
) {}

