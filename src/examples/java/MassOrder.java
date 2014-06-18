/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

import org.ObjectLayout.StructuredArray;

public class MassOrder extends StructuredArray<SimpleOrder> {

    private long accountID;
    private long instructionID;
    private long orderType;

    public static MassOrder newInstance(final long length,
                                        final long accountID, final long instructionID, final long orderType) {
        MassOrder massOrder;
        synchronized (MassOrder.class) {
            massOrder = (MassOrder) StructuredArray.newSubclassInstance(MassOrder.class,
                    SimpleOrder.class, length, simpleOrderConstructorArgTypes, accountID);
        }

        massOrder.accountID = accountID;
        massOrder.instructionID = instructionID;
        massOrder.orderType = orderType;

        return massOrder;
    }

    public SimpleOrder get(final long index) {
        return super.get(index);
    }

    public long getAccountID() {
        return accountID;
    }

    public long getInstructionID() {
        return instructionID;
    }

    public long getOrderType() {
        return orderType;
    }

    static final Class simpleOrderConstructorArgTypes[] = {long.class};


}
