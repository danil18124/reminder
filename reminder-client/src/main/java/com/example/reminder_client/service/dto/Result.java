/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.reminder_client.service.dto;

/**
 *
 * @author danil
 */
public record Result<T>(T data, ApiErrorResponse<?> error) {

    public static <T> Result<T> success(T data) {
        return new Result<>(data, null);
    }

    public static <T> Result<T> failure(ApiErrorResponse<?> error) {
        return new Result<>(null, error);
    }

    public boolean isSuccess() {
        return error == null;
    }
}

