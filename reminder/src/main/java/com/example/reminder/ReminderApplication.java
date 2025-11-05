package com.example.reminder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ReminderApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReminderApplication.class, args);
		

		// docker run --name postgres-reminder -e POSTGRES_DB=reminder -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres:16

		
		
//		INSERT INTO public.users (
//				id, email, provider, provider_id, role, created_at, updated_at) 
//				VALUES (1, 'test@test.com', 'LOCAL', 'test-id', 'USER', now(), now());

		
		
		
		// Для реализации отправки писем на почту была включена двухфакторная аутентификация в гугле.
		
		// Created app in google passwords for app: Mail
		// password: rehf uxvm cana gvts
		
		
		
		
		// при использовании @Scheduled приложение будет раз в какой-то период ходить в БД и делать запросы для проверки, наступило ли время уведомления.
		
		// Quartz регистрирует каждую задачу заранее и "будит" её ровно в нужный момент — сам, без опросов.
		// Quartz также использует БД, но для других целей. Ему нужна БД, чтобы не потерять триггеры при перезапуске приложения, а также для кластеризации.
		

		
		
		// Client-id: 1092248815122-p9cqi83pb0uj7rui7habghm9s26jdm00.apps.googleusercontent.com
		// Client-secret: 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
	}

}
