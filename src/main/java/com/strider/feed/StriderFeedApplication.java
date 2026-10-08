package com.strider.feed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {
		"com.strider.feed",
		"com.strider.strider_common_lib"
})
@EnableFeignClients(basePackages = "com.strider.feed.client")
public class StriderFeedApplication {

	public static void main(String[] args) {
		SpringApplication.run(StriderFeedApplication.class, args);
	}

}
