package com.example.app;

import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@SpringBootApplication
@RestController
public class AppApplication {
    Logger logger = LoggerFactory.getLogger(AppApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(AppApplication.class, args);
    }

    @GetMapping("/init")
    public String root(@RequestParam(value = "name", defaultValue = "World") String name,
            @RequestHeader HttpHeaders headers) {
        logger.error(headers.toString());
        logger.error(String.format("Hello %s!!", name));
        logger.debug("Debugging log");
        logger.info("Info log");
        logger.warn("Hey, This is a warning!");
        logger.error("Oops! We have an Error. OK");
        return String.format("Hello %s!!", name);
    }

    @GetMapping("/order")
    public ResponseEntity<?> get_order(@RequestParam(value = "name", defaultValue = "World") String name)
            throws InterruptedException {
        logger.info("GET -> /order, get_order reqst rceived");
        Thread.sleep(1000);
        int rand = getRandomNumber(2, 100);
        if (rand < 50) {
            logger.info("get_order order not found");
            return  ResponseEntity.status(HttpStatus.SC_SERVICE_UNAVAILABLE).body("get_order order not found");
        }
        logger.info("get_order");
        return ResponseEntity.ok("get_order") ;
    }

    @PutMapping("/order")
    public Integer put_order(@RequestParam(value = "name", defaultValue = "World") String name) {
        logger.info("math function PUT -> /order, put_order request received");
        int i = 0;
        try {
            i = 2 / 0;
        } catch (Exception e) {
            logger.error("arthmetic exception received:" + e.toString());
            System.out.println("arthmetic exception received:" + e.toString());
        }
        logger.info("put_order func ended");
        return i;
    }

    @PostMapping("/order")
    public String new_order(@RequestParam(value = "name", defaultValue = "World") String name) {
        logger.info("new_order func POST -> /order, request received");
        return "new_order";
    }

    @DeleteMapping("/order")
    public String delete_order(@RequestParam(value = "name", defaultValue = "World") String name) {
        logger.info("delete_order func DELETE -> /order, request received");
        return "delete_order";
    }

    @GetMapping("/total_order")
    public String total_order(@RequestParam(value = "name", defaultValue = "World") String name) {
        logger.info("GET -> /total_order, total_order reqst rceived");
        logger.info("total_order");
        return "total_order";
    }

    @GetMapping("/rejected_order")
    public String rejected_order(@RequestParam(value = "name", defaultValue = "World") String name) {
        logger.info("GET -> /rejected_order, total_order reqst rceived");
        int i = getRandomNumber(1, 200);
        logger.info("rejected_order number :" + i + " : duplicate order");
        return "rejected_order";
    }

    @GetMapping("/cpu_task")
    public String cpu_task(@RequestParam(value = "name", defaultValue = "World") String name) {
        for (int i = 0; i < 100; i++) {
            int tmp = i * i * i;
        }
        logger.info("cpu_task");
        return "cpu_task";
    }

    @GetMapping("/random_sleep")
    public String random_sleep(@RequestParam(value = "name", defaultValue = "World") String name)
            throws InterruptedException {
        Thread.sleep((int) (Math.random() / 5 * 10000));
        logger.info("random_sleep");
        return "random_sleep";
    }

    @GetMapping("/random_status")
    public String random_status(@RequestParam(value = "name", defaultValue = "World") String name,
            HttpServletResponse response) throws InterruptedException {
        List<Integer> givenList = Arrays.asList(200, 406, 304, 307, 403,402);
        Random rand = new Random();
        int randomElement = givenList.get(rand.nextInt(givenList.size()));
        response.setStatus(randomElement);
        logger.info("random_status");
        return "random_status";
    }

    @GetMapping("/chain")
    public String chain(@RequestParam(value = "name", defaultValue = "World") String name)
            throws InterruptedException, IOException {
        String TARGET_ONE_SVC = System.getenv().getOrDefault("TARGET_ONE_SVC", "localhost:8080");
        String TARGET_TWO_SVC = System.getenv().getOrDefault("TARGET_TWO_SVC", "localhost:8080");
        logger.debug("chain is starting");
        Request.Get("http://localhost:8080/")
                .execute().returnContent();
        Request.Get(String.format("http://%s/io_task", TARGET_ONE_SVC))
                .execute().returnContent();
        Request.Get(String.format("http://%s/cpu_task", TARGET_TWO_SVC))
                .execute().returnContent();
        logger.debug("chain is finished");
        return "chain";
    }

    @GetMapping("/error_test")
    public String error_test(@RequestParam(value = "name", defaultValue = "World") String name) throws Exception {
        throw new Exception("Error test");
    }

    public Integer getRandomNumber(int minValue, int maxValue) {
        Random random = new Random();
        int randomNumber = random.nextInt((maxValue - minValue) + 1) + minValue;
        return randomNumber;
    }
}
