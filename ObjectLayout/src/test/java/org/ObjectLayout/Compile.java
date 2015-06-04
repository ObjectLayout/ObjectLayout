/*
 * Written by Jaroslav Tulach, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import static org.junit.Assert.fail;

final class Compile implements DiagnosticListener<JavaFileObject> {
    private final List<Diagnostic<? extends JavaFileObject>> errors = new ArrayList<>();
    private final Map<String, byte[]> classes;
    private final String pkg;
    private final String cls;

    private Compile(String code) throws IOException {
        this.pkg = findPkg(code);
        this.cls = findCls(code);
        classes = compile(code);
    }

    /** Performs compilation of given Java code
     */
    public static Compile create(String code) throws IOException {
        return new Compile(code);
    }
    
    /** Checks for given class among compiled resources */
    public byte[] get(String res) {
        return classes.get(res);
    }
    
    /** Obtains errors created during compilation.
     */
    public List<Diagnostic<? extends JavaFileObject>> getErrors() {
        List<Diagnostic<? extends JavaFileObject>> err = new ArrayList<>();
        for (Diagnostic<? extends JavaFileObject> diagnostic : errors) {
            if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                err.add(diagnostic);
            }
        }
        return err;
    }
    
    private Map<String, byte[]> compile(final String code) throws IOException {
        StandardJavaFileManager sjfm = ToolProvider.getSystemJavaCompiler().getStandardFileManager(this, null, null);

        final Map<String, ByteArrayOutputStream> class2BAOS = new HashMap<>();

        JavaFileObject file = new Mem(URI.create("mem://mem"), Kind.SOURCE, code);
        
        final URI scratch;
        try {
            scratch = new URI("mem://mem3");
        } catch (URISyntaxException ex) {
            throw new IOException(ex);
        }
        
        JavaFileManager jfm = new ForwardingJavaFileManagerImpl(sjfm, class2BAOS, scratch);

        ToolProvider.getSystemJavaCompiler().getTask(null, jfm, this, /*XXX:*/Arrays.asList("-source", "1.7", "-target", "1.7"), null, Arrays.asList(file)).call();

        Map<String, byte[]> result = new HashMap<>();

        for (Map.Entry<String, ByteArrayOutputStream> e : class2BAOS.entrySet()) {
            result.put(e.getKey(), e.getValue().toByteArray());
        }

        return result;
    }


    @Override
    public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
        errors.add(diagnostic);
    }
    private static String findPkg(String java) throws IOException {
        Pattern p = Pattern.compile("package\\p{javaWhitespace}*([\\p{Alnum}\\.]+)\\p{javaWhitespace}*;", Pattern.MULTILINE);
        Matcher m = p.matcher(java);
        if (!m.find()) {
            throw new IOException("Can't find package declaration in the java file");
        }
        String pkg = m.group(1);
        return pkg;
    }
    private static String findCls(String java) throws IOException {
        Pattern p = Pattern.compile("class\\p{javaWhitespace}*([\\p{Alnum}\\.]+)\\p{javaWhitespace}", Pattern.MULTILINE);
        Matcher m = p.matcher(java);
        if (!m.find()) {
            throw new IOException("Can't find package declaration in the java file");
        }
        String cls = m.group(1);
        return cls;
    }

    void assertError(String msg) {
        if (getErrors().isEmpty()) {
            fail(msg + " there should be no errors");
        }
        StringBuilder sb = new StringBuilder();
        for (Diagnostic<? extends JavaFileObject> err : errors) {
            final String txt = err.getMessage(Locale.US);
            if (txt.contains(msg)) {
                return;
            }
            sb.append("\n").append(txt);
        }
        fail(msg + sb);
    }

    private class ForwardingJavaFileManagerImpl extends ForwardingJavaFileManager<JavaFileManager> {

        private final Map<String, ByteArrayOutputStream> class2BAOS;
        private final URI scratch;

        public ForwardingJavaFileManagerImpl(JavaFileManager fileManager, Map<String, ByteArrayOutputStream> class2BAOS, URI scratch) {
            super(fileManager);
            this.class2BAOS = class2BAOS;
            this.scratch = scratch;
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling) throws IOException {
            if (kind  == Kind.CLASS) {
                final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                
                class2BAOS.put(className.replace('.', '/') + ".class", buffer);
                return new Sibling(sibling.toUri(), kind, buffer);
            }
            
            if (kind == Kind.SOURCE) {
                return new Source(scratch/*sibling.toUri()*/, kind);
            }
            
            throw new IllegalStateException();
        }

            @Override
            public FileObject getFileForInput(Location location, String packageName, String relativeName) throws IOException {
                return null;
            }

      private class Sibling extends SimpleJavaFileObject {
            private final ByteArrayOutputStream buffer;

            public Sibling(URI uri, Kind kind, ByteArrayOutputStream buffer) {
                super(uri, kind);
                this.buffer = buffer;
            }

            @Override
            public OutputStream openOutputStream() throws IOException {
                return buffer;
            }
        }

      private class Source extends SimpleJavaFileObject {
            public Source(URI uri, Kind kind) {
                super(uri, kind);
            }
            private final ByteArrayOutputStream data = new ByteArrayOutputStream();

            @Override
            public OutputStream openOutputStream() throws IOException {
                return data;
            }

            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
                data.close();
                return new String(data.toByteArray());
            }
        }
    }

    private static class Mem extends SimpleJavaFileObject {

        private final String code;

        public Mem(URI uri, Kind kind, String code) {
            super(uri, kind);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return code;
        }
    }
}
