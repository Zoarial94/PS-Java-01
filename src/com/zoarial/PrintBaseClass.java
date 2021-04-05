package com.zoarial;

public abstract class PrintBaseClass {
    String prefix;
    PrintBaseClass(String str) {
        prefix = str;
    }
    protected <T> void println(T var) {
        System.out.print(prefix + ": ");
        System.out.println(var);
    }
    protected static void println() {
        System.out.println();
    }

}
