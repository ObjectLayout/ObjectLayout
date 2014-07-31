/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

class ArrayConstructionArgs {
    final CtorAndArgs arrayCtorAndArgs;
    final Class elementClass;
    final AbstractCtorAndArgsProvider ctorAndArgsProvider;
    final long[] lengths;
    long[] containingIndex;

    ArrayConstructionArgs(final CtorAndArgs arrayCtorAndArgs,
                          final Class elementClass,
                          final AbstractCtorAndArgsProvider ctorAndArgsProvider,
                          final long[] lengths,
                          final long[] containingIndex) {
        this.arrayCtorAndArgs = arrayCtorAndArgs;
        this.elementClass = elementClass;
        this.ctorAndArgsProvider = ctorAndArgsProvider;
        this.lengths = lengths;
        this.containingIndex = containingIndex;
    }

    ArrayConstructionArgs(ArrayConstructionArgs args) {
        this(args.arrayCtorAndArgs, args.elementClass, args.ctorAndArgsProvider, args.lengths, args.containingIndex);
    }
}
