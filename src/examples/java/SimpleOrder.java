/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

public class SimpleOrder {

    final long accountID;

    long symbolID;
    long amount;

    public SimpleOrder(final long accountID) {
        this.accountID = accountID;
    }

    public long getAccountID() {
        return accountID;
    }

    public long getSymbolID() {
        return symbolID;
    }

    public void setSymbolID(long symbolID) {
        this.symbolID = symbolID;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }
}
