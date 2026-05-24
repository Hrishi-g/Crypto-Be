package com.practice.firstapp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SingletonLogger {
    private static final Logger logger = LoggerFactory.getLogger(SingletonLogger.class);
    private static SingletonLogger instance;

    private SingletonLogger() {
    }

    public static SingletonLogger log() {
        if (instance == null) {
            instance = new SingletonLogger();
        }
        return instance;
    }

    public void log(String msg) {
        logger.info(msg);
    }

    public void info(String msg) {
        logger.info(msg);
    }

    public void info(String msg, Object... args) {
        logger.info(msg, args);
    }

    public void debug(String msg) {
        logger.debug(msg);
    }

    public void debug(String msg, Object... args) {
        logger.debug(msg, args);
    }

    public void warn(String msg) {
        logger.warn(msg);
    }

    public void warn(String msg, Object... args) {
        logger.warn(msg, args);
    }

    public void error(String msg) {
        logger.error(msg);
    }

    public void error(String msg, Object... args) {
        logger.error(msg, args);
    }

    public void error(String msg, Throwable t) {
        logger.error(msg, t);
    }
}
