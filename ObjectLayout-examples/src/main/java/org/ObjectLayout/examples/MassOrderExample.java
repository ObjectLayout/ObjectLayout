package org.ObjectLayout.examples;/*
 * Written by Michael Barker, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

public class MassOrderExample {
    public static void main(String[] args) {
        MassOrder massOrder = MassOrder.builder().
                accountId(1).
                instructionId("ABC").
                instrumentId(2).
                orderType(SimpleOrder.OrderType.LIMIT).
                addBid(10, 10).
                addBid(11, 10).
                addBid(12, 10).
                addBid(13, 10).
                addAsk(17, 10).
                addAsk(18, 10).
                addAsk(19, 10).
                addAsk(20, 10).newInstance();

        System.out.println(massOrder);

        for (SimpleOrder order : massOrder.getBids()) {
            System.out.println(order);
        }

        for (SimpleOrder order : massOrder.getAsks()) {
            System.out.println(order);
        }
    }
}
