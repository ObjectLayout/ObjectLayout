/*
 * Written by Michael Barker, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */



public class SimpleOrderWithBuilder {
	private final long instrumentId;
	private final long accountId;
	private final MassOrderWithBuilder.OrderType orderType;
	private final long price;
	private final long quantity;

	private long cancelledQuantity = 0;
	private long filledQuantity = 0;

	public SimpleOrderWithBuilder(MassOrderWithBuilder.Builder builder, long price, long quantity) {
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

	public MassOrderWithBuilder.OrderType getOrderType() {
		return this.orderType;
	}

	public long getPrice() {
		return this.price;
	}

	public long getQuantity() {
		return this.quantity;
	}
}
