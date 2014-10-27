/*
 * package-info.java
 *
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

/**
 * <h3>ObjectLayout: An optimised memory layout package.</h3>
 * <p>
 * The ObjectLayout package provides some key data structure classes designed with optimised memory layout
 * in mind. These classes are aimed at matching the natural speed benefits similar data structure
 * constructs enable in most C-style languages, while maintaining an idiomatic Java feel and a natural
 * fit with existing code and libraries.
 * <p>
 * The package classes provide full functionality on all JVMs (of Java SE 5 or above) at reasonable layouts
 * and execution speeds. However, the classes are carefully designed with semantics that would allow an
 * optimised JVM to implement them with improved memory layout not directly expressible in Java.
 * <p>
 * The {@link org.ObjectLayout.StructuredArray} class is a good example of this design pattern.
 * StructuredArray is carefully designed to allow a JVM to store it in memory with a layout similar to
 * an array of structs in C-like languages. When a JVM optimises StructuredArray in such a way, array
 * access benefits from both direct (as opposed to de-referenced) dead-reckoning index access, as well
 * as from fixed memory strides during streaming operations.
 * <p>
 * The three commonly used C-style container layout forms that ObjectLayout seeks to enable in Java are:
 * <ul>
 * <li>An array of structs: struct foo[];  </li>
 * <li>A struct with a struct inside: struct foo { int a; bar b; int c; }; </li>
 * <li>A struct with an array at the end: struct foo { int len; char[] payload; }; </li>
 * </ul>
 * <p>
 * The speed benefits in these three forms of layout derive from two dominant benefits:
 * <ol>
 * <li>1. Dead reckoning: In all three forms, the address of the target data field accessed
 * through the containing object can be directly derived from the containing object reference
 * without a data-dependent load operation (no de-referencing or equivalent operation needed).</li>
 * <li>2. Streaming: In the case of an array of structs, sequential access through multiple members
 * of the containing array result in predictable striding access in memory, enabling prefetch logic
 * (hardware assisted or otherwise) to compensate for much of the latency involved in cache misses.</li>
 * </ol>
 * <h3>The matching ObjectLayout forms</h3>
 * <p>
 * {@link org.ObjectLayout.StructuredArray} provides an idiomatic Java collection form
 * with speed (and semantics) similar to an "array of structs" form, supporting any constructable
 * java Object as an array member.
 * <p>
 * {@link org.ObjectLayout.Intrinsic @Intrinsic} provides an idiomatic Java means for declaring
 * member objects that are intrinsic to the instances of the class they are declared in.
 * {@link org.ObjectLayout.Intrinsic @Intrinsic} provides a "struct in struct" equivalent
 * relationship between Java objects, exposing the speed and layout benefits similar to the same
 * form in the C family languages.
 * <p>
 * Sub-classable Primitive and Reference array classes (e.g. {@link org.ObjectLayout.PrimitiveLongArray}
 * and {@link org.ObjectLayout.ReferenceArray}) provide an idiomatic Java means of
 * declaring constructs with speed (and semantics) similar to "struct with array at the end". They do
 * so by supporting the subclassing of arrays of the various primitive and reference forms possible
 * in Java. {@link org.ObjectLayout.StructuredArray} similarly supports this capability (via
 * subclassing) for the equivalent of "struct with array of structs at the end".
 *
 *
 */

package org.ObjectLayout;


