/*
 * Written by Gil Tene, Martin Thompson, and Michael Barker, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.lang.reflect.Constructor;

import org.ObjectLayout.CtorAndArgs;
import org.ObjectLayout.CtorAndArgsProvider;
import org.ObjectLayout.StructuredArray;

public class MassOrderWithBuilder extends StructuredArray<SimpleOrderWithBuilder> {

    private final long accountId;
    private final String instructionId;
    private final OrderType orderType;
	private long instrumentId;

    MassOrderWithBuilder(final Builder builder) {
        this.accountId = builder.accountId;
        this.instructionId = builder.instructionId;
        this.orderType = builder.orderType;
        this.instrumentId = builder.instrumentId;
    }

    public long getAccountId() {
        return accountId;
    }

    public String getInstructionId() {
        return instructionId;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public long getInstrumentId() {
	return instrumentId;
    }

    public static enum OrderType {
	MARKET, LIMIT
    }

    private static final ThreadLocal<Builder> BUILDER = new ThreadLocal<MassOrderWithBuilder.Builder>() {
	protected Builder initialValue() {
		try {
				return new Builder();
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
	};
    };

    public static Builder builder() {
	return BUILDER.get();
    }

    public static class Builder extends CtorAndArgsProvider<SimpleOrderWithBuilder> {

        private static CtorAndArgs<MassOrderWithBuilder> massOrderCtorAndArgs() {
            try {
			final Class[] massOrderConstructorArgTypes = {Builder.class};
			final Constructor<MassOrderWithBuilder> massOrderConstructor;
			massOrderConstructor = MassOrderWithBuilder.class.getConstructor(massOrderConstructorArgTypes);
			return new CtorAndArgs<MassOrderWithBuilder>(massOrderConstructor, (Object[]) null);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
        }

        private static CtorAndArgs<SimpleOrderWithBuilder> simpleOrderArgs() {
		try {
			final Constructor<SimpleOrderWithBuilder> simpleOrderConstructor =
					SimpleOrderWithBuilder.class.getConstructor(Builder.class, long.class, long.class);
			return new CtorAndArgs<SimpleOrderWithBuilder>(simpleOrderConstructor, null, 0L, 0L);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
        }

		private static final int MAX_ORDERS_PER_SIDE = 6;

		long accountId;
	long instrumentId;
	String instructionId;
	OrderType orderType;

	private int bidIndex;
	private long[] bidPrices = new long[MAX_ORDERS_PER_SIDE];
	private long[] bidQuantities = new long[MAX_ORDERS_PER_SIDE];

	private int askIndex;
	private long[] askPrices = new long[MAX_ORDERS_PER_SIDE];
	private long[] askQuantities = new long[MAX_ORDERS_PER_SIDE];

	private final CtorAndArgs<MassOrderWithBuilder> massOrderArgs = massOrderCtorAndArgs();
	private CtorAndArgs<SimpleOrderWithBuilder> simpleOrderArgs;

	public Builder() throws NoSuchMethodException {
		super(SimpleOrderWithBuilder.class);
		massOrderArgs.setArgs(this);
	}

	public Builder accountId(long accountId) {
		this.accountId = accountId;
		return this;
	}

	public Builder instrumentId(long instrumentId) {
		this.instrumentId = instrumentId;
		return this;
	}

	public Builder instructionId(String instructionId) {
		this.instructionId = instructionId;
		return this;
	}

	public Builder orderType(OrderType orderType) {
		this.orderType = orderType;
		return this;
	}

	public Builder addBid(long price, long quantity) {
		bidPrices[bidIndex] = price;
		bidQuantities[bidIndex] = quantity;
		bidIndex++;
		return this;
	}

	public Builder addAsk(long price, long quantity) {
		askPrices[askIndex] = price;
		askQuantities[askIndex] = quantity;
		askIndex++;
		return this;
	}

	@Override
	public CtorAndArgs<SimpleOrderWithBuilder> getForIndices(long[] indices)
			throws NoSuchMethodException {

		CtorAndArgs<SimpleOrderWithBuilder> args = simpleOrderArgs != null ? simpleOrderArgs : simpleOrderArgs();

		long index = indices[0];

		if (index > bidIndex + askIndex) {
			throw new IllegalArgumentException();
		}

		int i = (int) index;
		if (i < bidIndex) {
			args.setArgs(this, bidPrices[i], bidQuantities[i]);
		} else {
			i = i - bidIndex;
			args.setArgs(this, askPrices[i], askQuantities[i]);
		}

		return args;
	}

	@SuppressWarnings("unchecked")
		@Override
	public void recycle(CtorAndArgs ctorAndArgs) {
		this.simpleOrderArgs = ctorAndArgs;
	}

	public MassOrderWithBuilder newInstance() {

		// TODO: Order validation and sorting by price.

		long length = bidIndex + askIndex;
		return (MassOrderWithBuilder) StructuredArray.newSubclassInstance(massOrderArgs, this, length);
	}
    }
}
