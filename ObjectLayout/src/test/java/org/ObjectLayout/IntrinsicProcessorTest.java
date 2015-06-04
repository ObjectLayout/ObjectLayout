/*
 * Written by Jaroslav Tulach, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */
package org.ObjectLayout;

import org.junit.Test;

public class IntrinsicProcessorTest {

    @Test public void nonPrivateIntrinsicObjectMember() throws Exception {
        Compile c = Compile.create(
"package org.ObjectLayoutTest;\n" +
"import org.ObjectLayout.*;\n" +
"class BadContainerNonPrivate {\n" +
"    @Intrinsic\n" +
"    final Point intrinsicPoint = IntrinsicObjects.constructWithin(\"intrinsicPoint\", this);\n" +
"\n" +
"    Point getPoint() {\n" +
"        return intrinsicPoint;\n" +
"    }\n" +
"}\n" +
"class Point {\n" +
"}\n"
        );
        c.assertError("@Intrinsic object annotations can only be declared for private final fields");
    }
    
    @Test public void nonFinalIntrinsicObjectMember() throws Exception {
        Compile c = Compile.create(
"package org.ObjectLayoutTest;\n" +
"import org.ObjectLayout.*;\n" +
"class BadContainerNonFinal {\n" +
"    @Intrinsic\n" +
"    private Point intrinsicPoint = IntrinsicObjects.constructWithin(\"intrinsicPoint\", this);\n" +
"\n" +
"    Point getPoint() {\n" +
"        return intrinsicPoint;\n" +
"    }\n" +
"}\n" +
"class Point {\n" +
"}\n"
        );
        c.assertError("@Intrinsic object annotations can only be declared for private final fields");
    }
    
    
}
