package com.example;

/**
 * A simple fixture class for testing.
 */
public class Greeter {
    public String greet(String name) {
        return "Hello, " + name + "!";
    }

    public void printGreeting(String name) {
        String message = greet(name);
        System.out.println(message);
    }
}

