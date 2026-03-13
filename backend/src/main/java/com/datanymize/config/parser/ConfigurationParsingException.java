package com.datanymize.config.parser;

/**
 * Exception thrown when configuration parsing fails.
 */
public class ConfigurationParsingException extends Exception {
    private int line;
    private int column;
    private String errorCode;

    public ConfigurationParsingException(String message) {
        super(message);
        this.line = -1;
        this.column = -1;
    }

    public ConfigurationParsingException(String message, Throwable cause) {
        super(message, cause);
        this.line = -1;
        this.column = -1;
    }

    public ConfigurationParsingException(String message, int line, int column) {
        super(message);
        this.line = line;
        this.column = column;
    }

    public ConfigurationParsingException(String message, int line, int column, String errorCode) {
        super(message);
        this.line = line;
        this.column = column;
        this.errorCode = errorCode;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        if (line >= 0 && column >= 0) {
            return String.format("ConfigurationParsingException at line %d, column %d: %s", 
                line, column, getMessage());
        }
        return "ConfigurationParsingException: " + getMessage();
    }
}
