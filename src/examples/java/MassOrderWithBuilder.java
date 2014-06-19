/*
 * Written by Gil Tene, Martin Thompson, and Michael Barker, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.lang.reflect.Constructor;
import java.util.Iterator;

import org.ObjectLayout.CtorAndArgs;
import org.ObjectLayout.CtorAndArgsProvider;
import org.ObjectLayout.StructuredArray;

public class MassOrderWithBuilder extends
        StructuredArray<SimpleOrderWithBuilder> {

    private final long accountId;
    private final String instructionId;
    private final SimpleOrderWithBuilder.OrderType orderType;
    private final long instrumentId;
    private final int askOrderIndex;

    MassOrderWithBuilder(final Builder builder) {
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

    public SimpleOrderWithBuilder.OrderType getOrderType() {
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

    private final Iterable<SimpleOrderWithBuilder> bidIterable = new Iterable<SimpleOrderWithBuilder>() {
        @Override
        public Iterator<SimpleOrderWithBuilder> iterator() {
            return new StructuredIterator<SimpleOrderWithBuilder>(
                    MassOrderWithBuilder.this, getBidOrderIndex(), getBidOrderCount());
        }
    };

    public Iterable<SimpleOrderWithBuilder> getBids() {
        return bidIterable;
    }

    public int getAskOrderCount() {
        return (int) (getLength() - askOrderIndex);
    }

    public int getAskOrderIndex() {
        return askOrderIndex;
    }

    private final Iterable<SimpleOrderWithBuilder> askIterable = new Iterable<SimpleOrderWithBuilder>() {
        @Override
        public Iterator<SimpleOrderWithBuilder> iterator() {
            return new StructuredIterator<SimpleOrderWithBuilder>(
                    MassOrderWithBuilder.this, getAskOrderIndex(), getAskOrderCount());
        }
    };

    public Iterable<SimpleOrderWithBuilder> getAsks() {
        return askIterable;
    }

    @Override
    public String toString() {
        return "MassOrderWithBuilder [accountId=" + this.accountId
                + ", instructionId=" + this.instructionId + ", orderType="
                + this.orderType + ", instrumentId=" + this.instrumentId
                + ", askOrderIndex=" + this.askOrderIndex + "]";
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

    public static class Builder extends
            CtorAndArgsProvider<SimpleOrderWithBuilder> {

        private static CtorAndArgs<MassOrderWithBuilder> massOrderCtorAndArgs() {
            try {
                final Constructor<MassOrderWithBuilder> massOrderConstructor =
                        MassOrderWithBuilder.class.getDeclaredConstructor(Builder.class);
                massOrderConstructor.setAccessible(true);
                return new CtorAndArgs<MassOrderWithBuilder>(
                        massOrderConstructor, new Object[] { null });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }


        private static CtorAndArgs<SimpleOrderWithBuilder> simpleOrderArgs() {
            try {
                final Constructor<SimpleOrderWithBuilder> simpleOrderConstructor = SimpleOrderWithBuilder.class
                        .getConstructor(Builder.class, SimpleOrderWithBuilder.Side.class, long.class, long.class);
                return new CtorAndArgs<SimpleOrderWithBuilder>(
                        simpleOrderConstructor, null, 0L, 0L);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private static final int MAX_ORDERS_PER_SIDE = 6;

        long accountId;
        long instrumentId;
        String instructionId;
        SimpleOrderWithBuilder.OrderType orderType;

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

        public Builder orderType(SimpleOrderWithBuilder.OrderType orderType) {
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
        public CtorAndArgs<SimpleOrderWithBuilder> getForIndices(
                long... indices) throws NoSuchMethodException {

            CtorAndArgs<SimpleOrderWithBuilder> args = 
                    simpleOrderArgs != null ? simpleOrderArgs : simpleOrderArgs();

            long index = indices[0];

            if (index > bidIndex + askIndex) {
                throw new IllegalArgumentException();
            }

            int i = (int) index;
            if (i < bidIndex) {
                args.setArgs(this, SimpleOrderWithBuilder.Side.BID, bidPrices[i], bidQuantities[i]);
            } else {
                i = i - bidIndex;
                args.setArgs(this, SimpleOrderWithBuilder.Side.ASK, askPrices[i], askQuantities[i]);
            }

            return args;
        }

        @Override
        public void recycle(CtorAndArgs<SimpleOrderWithBuilder> ctorAndArgs) {
            this.simpleOrderArgs = ctorAndArgs;
        }

        public MassOrderWithBuilder newInstance() {

            // TODO: Order validation and sorting by price.

            long length = bidIndex + askIndex;
            return (MassOrderWithBuilder) StructuredArray.newSubclassInstance(
                    massOrderArgs, this, length);
        }
    }
}
