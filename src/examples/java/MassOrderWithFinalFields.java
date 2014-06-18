/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

import org.ObjectLayout.CtorAndArgs;
import org.ObjectLayout.SingletonCtorAndArgsProvider;
import org.ObjectLayout.StructuredArray;

import java.lang.reflect.Constructor;

public class MassOrderWithFinalFields extends StructuredArray<SimpleOrder> {

    private final long accountID;
    private final long instructionID;
    private final long orderType;

    public static MassOrderWithFinalFields newInstance(final long length,
                                        final long accountID, final long instructionID, final long orderType) {
        synchronized (MassOrderWithFinalFields.class) {
            massOrderCtorAndArgs.setArgs(accountID, instructionID, orderType);
            simpleOrderCtorAndArgsProvider.setArgs(accountID);
            return (MassOrderWithFinalFields) StructuredArray.newSubclassInstance(
                    massOrderCtorAndArgs, simpleOrderCtorAndArgsProvider, length);
        }
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

    MassOrderWithFinalFields(final long accountID, final long instructionID, final long orderType) {
        this.accountID = accountID;
        this.instructionID = instructionID;
        this.orderType = orderType;
    }

    static final CtorAndArgs<MassOrderWithFinalFields> massOrderCtorAndArgs;
    static final SingletonCtorAndArgsProvider<SimpleOrder> simpleOrderCtorAndArgsProvider;

    static {
        try {
            final Class massOrderConstructorArgTypes[] = {long.class, long.class, long.class};
            final Constructor<MassOrderWithFinalFields> massOrderConstructor;
            massOrderConstructor = MassOrderWithFinalFields.class.getConstructor(massOrderConstructorArgTypes);
            massOrderCtorAndArgs = new CtorAndArgs<MassOrderWithFinalFields>(massOrderConstructor, 0, 0, 0);

            final Class simpleOrderConstructorArgTypes[] = {long.class};
            simpleOrderCtorAndArgsProvider =
                    new SingletonCtorAndArgsProvider<SimpleOrder>(SimpleOrder.class,
                            simpleOrderConstructorArgTypes, 0);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }
}
