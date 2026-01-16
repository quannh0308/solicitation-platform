package com.solicitation.common.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Structured logger that provides consistent JSON-formatted logging.
 * 
 * This class wraps SLF4J Logger and adds structured data support for
 * better log analysis and monitoring.
 * 
 * Requirements:
 * - Req 12.2: Structured logging with correlation IDs
 */
public class StructuredLogger {
    
    private final Logger logger;
    
    private StructuredLogger(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
    }
    
    private StructuredLogger(String name) {
        this.logger = LoggerFactory.getLogger(name);
    }
    
    /**
     * Creates a structured logger for the given class.
     * 
     * @param clazz The class to create a logger for
     * @return A new structured logger
     */
    public static StructuredLogger getLogger(Class<?> clazz) {
        return new StructuredLogger(clazz);
    }
    
    /**
     * Creates a structured logger with the given name.
     * 
     * @param name The logger name
     * @return A new structured logger
     */
    public static StructuredLogger getLogger(String name) {
        return new StructuredLogger(name);
    }
    
    /**
     * Logs an info message with structured data.
     * 
     * @param message The log message
     * @param data Additional structured data
     */
    public void info(String message, Map<String, Object> data) {
        if (logger.isInfoEnabled()) {
            logger.info(formatMessage(message, data));
        }
    }
    
    /**
     * Logs an info message.
     * 
     * @param message The log message
     */
    public void info(String message) {
        logger.info(message);
    }
    
    /**
     * Logs a warning message with structured data.
     * 
     * @param message The log message
     * @param data Additional structured data
     */
    public void warn(String message, Map<String, Object> data) {
        if (logger.isWarnEnabled()) {
            logger.warn(formatMessage(message, data));
        }
    }
    
    /**
     * Logs a warning message.
     * 
     * @param message The log message
     */
    public void warn(String message) {
        logger.warn(message);
    }
    
    /**
     * Logs a warning message with exception.
     * 
     * @param message The log message
     * @param throwable The exception
     */
    public void warn(String message, Throwable throwable) {
        logger.warn(message, throwable);
    }
    
    /**
     * Logs an error message with structured data.
     * 
     * @param message The log message
     * @param data Additional structured data
     */
    public void error(String message, Map<String, Object> data) {
        if (logger.isErrorEnabled()) {
            logger.error(formatMessage(message, data));
        }
    }
    
    /**
     * Logs an error message.
     * 
     * @param message The log message
     */
    public void error(String message) {
        logger.error(message);
    }
    
    /**
     * Logs an error message with exception.
     * 
     * @param message The log message
     * @param throwable The exception
     */
    public void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }
    
    /**
     * Logs an error message with exception and structured data.
     * 
     * @param message The log message
     * @param throwable The exception
     * @param data Additional structured data
     */
    public void error(String message, Throwable throwable, Map<String, Object> data) {
        if (logger.isErrorEnabled()) {
            logger.error(formatMessage(message, data), throwable);
        }
    }
    
    /**
     * Logs a debug message with structured data.
     * 
     * @param message The log message
     * @param data Additional structured data
     */
    public void debug(String message, Map<String, Object> data) {
        if (logger.isDebugEnabled()) {
            logger.debug(formatMessage(message, data));
        }
    }
    
    /**
     * Logs a debug message.
     * 
     * @param message The log message
     */
    public void debug(String message) {
        logger.debug(message);
    }
    
    /**
     * Checks if info logging is enabled.
     * 
     * @return true if info logging is enabled
     */
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }
    
    /**
     * Checks if debug logging is enabled.
     * 
     * @return true if debug logging is enabled
     */
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }
    
    /**
     * Creates a builder for structured log data.
     * 
     * @return A new data builder
     */
    public static DataBuilder data() {
        return new DataBuilder();
    }
    
    /**
     * Formats a message with structured data.
     * 
     * @param message The base message
     * @param data The structured data
     * @return The formatted message
     */
    private String formatMessage(String message, Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return message;
        }
        
        StringBuilder sb = new StringBuilder(message);
        sb.append(" | ");
        
        boolean first = true;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }
        
        return sb.toString();
    }
    
    /**
     * Builder for structured log data.
     */
    public static class DataBuilder {
        private final Map<String, Object> data = new HashMap<>();
        
        public DataBuilder add(String key, Object value) {
            if (value != null) {
                data.put(key, value);
            }
            return this;
        }
        
        public DataBuilder count(String key, int value) {
            data.put(key, value);
            return this;
        }
        
        public DataBuilder duration(String key, long milliseconds) {
            data.put(key + "Ms", milliseconds);
            return this;
        }
        
        public DataBuilder success(boolean success) {
            data.put("success", success);
            return this;
        }
        
        public Map<String, Object> build() {
            return new HashMap<>(data);
        }
    }
}
