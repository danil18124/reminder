/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.reminder.util;

/**
 *
 * @author danil
 */
import com.example.reminder.model.OAuthProvider;
import com.example.reminder.model.Role;
import com.example.reminder.model.User;
import org.springframework.test.util.ReflectionTestUtils;

public class TestUserFactory {

    public static User createUser(Long id, String email, OAuthProvider provider, String providerId) {
        User user = User.builder()
                .email(email)
                .provider(provider)
                .providerId(providerId)
                .role(Role.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
