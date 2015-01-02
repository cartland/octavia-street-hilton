package com.chriscartland.octaviastreethilton;

/**
 * The part of a transaction representing a debt to the purchaser.
 */
public class Debt {

    private String amount;

    private String debtor;

    public Debt(String amount, String debtor) {
        this.amount = amount;
        this.debtor = debtor;
    }
}
