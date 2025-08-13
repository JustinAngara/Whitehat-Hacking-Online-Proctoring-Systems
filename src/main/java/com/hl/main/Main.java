package com.hl.main;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Main {
    static WebSocketHandler handler;
    static PseudoType pt;
    static SecureFrame s;
    static GeminiAPI ga;

    public static void main(String[] args) throws Exception {
        // now we want to setup the api
        APIHandler.run();

        ga = new GeminiAPI();

        // psuedo type
        pt = new PseudoType();
        pt.setup();

        // creates frame
        s = new SecureFrame();
        s.run();

        // this will run KeyListener
        int coreCount = Runtime.getRuntime().availableProcessors();
        System.out.println(coreCount);

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(coreCount);

        // put in another thread so it works async
        KeyListener k = new KeyListener();
        executor.execute(k);


        // will run springboot
//        ConfigurableApplicationContext context = SpringApplication.run(WebSocketApplication.class, args);
//        handler = context.getBean(WebSocketHandler.class);

        // no longer point
        executor.shutdown();
    }
}
