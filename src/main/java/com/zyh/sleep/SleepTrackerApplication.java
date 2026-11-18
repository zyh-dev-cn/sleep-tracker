package com.zyh.sleep;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class SleepTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SleepTrackerApplication.class, args);
    }

    @GetMapping("/ping")
    public String ping() {
        return "sleep-tracker is running";
    }
}
