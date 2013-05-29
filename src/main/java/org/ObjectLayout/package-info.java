/*
 * package-info.java
 * Written by Gil Tene of Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 *
 */

/**
 * <h3>ObjectLayout: An optimised memory layout package.</h3>
 * <p>
 * The ObjectLayout package provides some core classes designed with optimised memory layout in mind.
 * The package classes provide full functionality on all JVMs (of Java SE 5 or above) at reasonable layouts
 * and execution speeds. However, the classes are carefully designed with semantics that would allow an
 * optimised JVM to implement them with improved memory layout not directly expressible in Java.
 * <p>
 * The {@link StructuredArray} class is a good example of this design pattern. StructuredArray is carefully designed
 * to allow a JVM to store it in memory with a layout similar to an array of structs in C-like languages.
 * When a JVM optimises StructuredArray in such a way, array access benefits from both direct (as opposed
 * to de-referenced) dead-reckoning index access, as well as from fixed memory strides during streaming
 * operations.
 */

package org.ObjectLayout;


