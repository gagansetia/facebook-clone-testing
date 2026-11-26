package com.facebookclone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;

/** Starts the local Facebook-style demo website at http://localhost:8080. */
@SpringBootApplication
public class FacebookCloneApplication {
    public static void main(String[] args) {
        SpringApplication.run(FacebookCloneApplication.class, args);
    }

    @EventListener
    public void showLocalLink(WebServerInitializedEvent event) {
        System.out.println("\nFaceConnect is running: http://localhost:" + event.getWebServer().getPort() + "\n");
    }
}
