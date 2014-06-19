/*
 * Written by Gil Tene, Martin Thompson, and Michael Barker, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.util.Iterator;

import org.ObjectLayout.CtorAndArgs;
import org.ObjectLayout.CtorAndArgsProvider;
import org.ObjectLayout.StructuredArray;

public class MassOrder extends
        StructuredArray<SimpleOrder> {

    private final long accountId;
    private final String instructionId;
    private final SimpleOrder.OrderType orderType;
    private final long instrumentId;
    private final int askOrderIndex;

    MassOrder(final Builder builder) {
        this.accountId = builder.accountId;
        this.instructionId = builder.instructionId;
        this.orderType = builder.orderType;
        this.instrumentId = builder.instrumentId;
        this.askOrderIndex = builder.bidIndex;
    }

    public long getAccountId() {
        return accountId;
    }

    public String getInstructionId() {
        return instructionId;
    }

    public SimpleOrder.OrderType getOrderType() {
        return orderType;
    }

    public long getInstrumentId() {
        return instrumentId;
    }

    public int getBidOrderIndex() {
        return 0;
    }

    public int getBidOrderCount() {
        return askOrderIndex;
    }

    private final Iterable<SimpleOrder> bidIterable = new Iterable<SimpleOrder>() {
        @Override
        public Iterator<SimpleOrder> iterator() {
            return new StructuredIterator<SimpleOrder>(
                    MassOrder.this, getBidOrderIndex(), getBidOrderCount());
        }
    };

    public Iterable<SimpleOrder> getBids() {
        return bidIterable;
    }

    public int getAskOrderCount() {
        return (int) (getLength() - askOrderIndex);
    }

    public int getAskOrderIndex() {
        return askOrderIndex;
    }

    private final Iterable<SimpleOrder> askIterable = new Iterable<SimpleOrder>() {
        @Override
        public Iterator<SimpleOrder> iterator() {
            return new StructuredIterator<SimpleOrder>(
                    MassOrder.this, getAskOrderIndex(), getAskOrderCount());
        }
    };

    public Iterable<SimpleOrder> getAsks() {
        return askIterable;
    }

    @Override
    public String toString() {
        return "MassOrderWithBuilder [accountId=" + this.accountId
                + ", instructionId=" + this.instructionId + ", orderType="
                + this.orderType + ", instrumentId=" + this.instrumentId
                + ", askOrderIndex=" + this.askOrderIndex + "]";
    }

    private static final ThreadLocal<Builder> BUILDER = new ThreadLocal<MassOrder.Builder>() {
        protected Builder initialValue() {
            try {
                return new Builder();
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        };
    };

    public static Builder builder() {
        return BUILDER.get().reset();
    }

    public static final class Builder extends
            CtorAndArgsProvider<SimpleOrder> {

        private static CtorAndArgs<MassOrder> massOrderCtorAndArgs() {
            try {
                final Class[] argTypes = { Builder.class };
                return new CtorAndArgs<MassOrder>(MassOrder.class, argTypes, (Object) null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private static CtorAndArgs<SimpleOrder> simpleOrderCtorAndArgs() {
            try {
                final Class[] argTypes = { Builder.class, SimpleOrder.Side.class, long.class, long.class };
                return new CtorAndArgs<SimpleOrder>(SimpleOrder.class, argTypes, null, null, 0L, 0L);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private static final int MAX_ORDERS_PER_SIDE = 6;

        long accountId;
        long instrumentId;
        String instructionId;
        SimpleOrder.OrderType orderType;

        private int bidIndex;
        private long[] bidPrices = new long[MAX_ORDERS_PER_SIDE];
        private long[] bidQuantities = new long[MAX_ORDERS_PER_SIDE];

        private int askIndex;
        private long[] askPrices = new long[MAX_ORDERS_PER_SIDE];
        private long[] askQuantities = new long[MAX_ORDERS_PER_SIDE];

        private final CtorAndArgs<MassOrder> massOrderCtorAndArgs = massOrderCtorAndArgs();
        private CtorAndArgs<SimpleOrder> simpleOrderCtorAndArgs;

        public Builder() throws NoSuchMethodException {
            super(SimpleOrder.class);
            massOrderCtorAndArgs.setArgs(this);
            reset();
        }

        public Builder reset() {
            accountId = 0;
            instrumentId = 0;
            instructionId = null;
            orderType = null;
            bidIndex = 0;
            askIndex = 0;

            return this;
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

        public Builder orderType(SimpleOrder.OrderType orderType) {
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
        public CtorAndArgs<SimpleOrder> getForIndex(
                long... indices) throws NoSuchMethodException {

            CtorAndArgs<SimpleOrder> ctorAndArgs =
                    simpleOrderCtorAndArgs != null ? simpleOrderCtorAndArgs : simpleOrderCtorAndArgs();

            long index = indices[0];

            if (index > bidIndex + askIndex) {
                throw new IllegalArgumentException();
            }

            int i = (int) index;
            if (i < bidIndex) {
                ctorAndArgs.setArgs(this, SimpleOrder.Side.BID, bidPrices[i], bidQuantities[i]);
            } else {
                i = i - bidIndex;
                ctorAndArgs.setArgs(this, SimpleOrder.Side.BID, askPrices[i], askQuantities[i]);
            }

            return ctorAndArgs;
        }

        @Override
        public void recycle(CtorAndArgs<SimpleOrder> ctorAndArgs) {
            this.simpleOrderCtorAndArgs = ctorAndArgs;
        }

        public MassOrder newInstance() {

            // TODO: Order validation and sorting by price.

            long length = bidIndex + askIndex;
            return (MassOrder) StructuredArray.newSubclassInstance(
                    massOrderCtorAndArgs, this, length);
        }
    }
}
