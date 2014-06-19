/*
 * Written by Michael Barker, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

public class SimpleOrderWithBuilder {

    public static enum OrderType {
        MARKET, LIMIT
    }

    public static enum Side {
        BID, ASK
    }

    private final long instrumentId;
    private final long accountId;
    private final SimpleOrderWithBuilder.OrderType orderType;
    private final long price;
    private final long quantity;
    private final Side side;

    private long cancelledQuantity = 0;
    private long filledQuantity = 0;

    public SimpleOrderWithBuilder(MassOrderWithBuilder.Builder builder,
            Side side, long price, long quantity) {
        this.side = side;
        this.instrumentId = builder.instrumentId;
        this.accountId = builder.accountId;
        this.orderType = builder.orderType;
        this.price = price;
        this.quantity = quantity;
    }

    public long getCancelledQuantity() {
        return this.cancelledQuantity;
    }

    public void updateCancelled(long delta) {
        this.cancelledQuantity -= delta;
    }

    public long getFilledQuantity() {
        return this.filledQuantity;
    }

    public void updateFill(long matchedQuantity) {
        this.filledQuantity -= matchedQuantity;
    }

    public long getInstrumentId() {
        return this.instrumentId;
    }

    public long getAccountId() {
        return this.accountId;
    }

    public SimpleOrderWithBuilder.OrderType getOrderType() {
        return this.orderType;
    }

    public long getPrice() {
        return this.price;
    }

    public long getQuantity() {
        return this.quantity;
    }

    @Override
    public String toString() {
        return "SimpleOrderWithBuilder [instrumentId=" + this.instrumentId
                + ", accountId=" + this.accountId + ", orderType="
                + this.orderType + ", side=" + this.side + ", price="
                + this.price + ", quantity=" + this.quantity
                + ", cancelledQuantity=" + this.cancelledQuantity
                + ", filledQuantity=" + this.filledQuantity + "]";
    }
}
