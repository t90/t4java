import javax.tools.*;
import java.io.*;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class TemplateProcessor implements Runnable {
    private String _inputTemplateName;
    private String _outputTemplateName;
    private boolean _deleteJavaFile = true;

    private enum State {
        Code,
        Text,
        Method,
        Variable,
        Import,
    }

    public void setDeleteJavaFile(boolean newValue){
        _deleteJavaFile = newValue;
    }

    static State _state = State.Text;

    final static StringBuilder _methods = new StringBuilder();
    final static StringBuilder _imports = new StringBuilder();


    public TemplateProcessor(String inputTemplateName, String outputTemplateName) {

        _inputTemplateName = inputTemplateName;
        _outputTemplateName = outputTemplateName;
    }


    private static String getTemplate() throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(EntryPoint.class.getResourceAsStream("/template.txt")));
        String str;
        StringBuilder sb = new StringBuilder();
        while ((str = r.readLine()) != null) {
            sb.append(str + "\n");
        }
        return sb.toString();
    }

    private static KeyValue<Integer, String> firstIndexOf(final String[] needles, final String hayStack) {
        Map<Integer, String> _matchesMap = new HashMap<Integer, String>();

        for (String needle : needles) {
            _matchesMap.put(hayStack.indexOf(needle), needle);
        }

        ArrayList<Integer> indexesList = new ArrayList<Integer>(_matchesMap.keySet());
        Collections.sort(indexesList);
        for (int retVal : indexesList) {
            if (retVal >= 0) {
                return new KeyValue<Integer, String>(retVal, _matchesMap.get(retVal));
            }
        }
        return null;
    }

    private static boolean processLine(String str, StringWriter out) throws Exception {
        int idx;
        switch (_state) {
            case Code:

                idx = str.indexOf("%>");
                if (idx >= 0) {
                    out.write(str, 0, idx);
                    _state = State.Text;
                    processLine(str.substring(idx + 2), out);
                } else {
                    out.write(str);
                }
                return false;
            case Method:
                idx = str.indexOf("%>");
                if (idx < 0) {
                    _methods.append(str + "\n");
                } else if (idx == 0) {
                    _state = State.Text;
                } else {
                    throw new Exception("Methods block must finish with '%>' mark");
                }
                return false;
            case Text:
                KeyValue<Integer, String> keyValue = firstIndexOf(_keys, str);
                if (keyValue == null) {
                    str = str.replace("\\", "\\\\");
                    out.write(String.format("out.write(\"%s\");", str));
                    out.write("\n");
                } else {
                    String passVal = str.substring(0, keyValue.First).replace("\\", "\\\\");
                    out.write(String.format("out.write(\"%s\");", passVal));
                    out.write("\n");
                    _state = _prefixMapping.get(keyValue.Second);
                    processLine(str.substring(keyValue.First + 3), out);
                }
                break;
            case Variable:
                _state = State.Text;
                idx = str.indexOf("%>");
                if (idx < 0) {
                    throw new Exception("Expecting '%>' in variable section. Canonical declaration is <%=variable %>");
                }
                out.write(String.format("out.write(\"\" + %s);", str.substring(0, idx)));
                processLine(str.substring(idx + 2), out);
                return false;
            case Import:
                idx = str.indexOf("%>");
                if (idx >= 0) {
                    _imports.append(str.substring(0, idx).trim() + "\n");
                    _state = State.Text;
                } else {
                    throw new Exception("Import block must finish with '%>' mark");
                }
                return false;
        }
        return true;
    }

    final static HashMap<String, State> _prefixMapping = new HashMap<String, State>();

    static {
        _prefixMapping.put("<%=", State.Variable);
        _prefixMapping.put("<%+", State.Method);
        _prefixMapping.put("<%%", State.Code);
        _prefixMapping.put("<%@", State.Import);
    }

    static String[] _keys;

    static {
        _keys = new String[_prefixMapping.size()];
        _keys = _prefixMapping.keySet().toArray(_keys);
    }


    private static class KeyValue<T1, T2> {
        KeyValue(T1 v1, T2 v2) {
            First = v1;
            Second = v2;
        }

        T1 First;
        T2 Second;

    }


    @Override
    public void run() {
        long lineNumber = 0;


        BufferedReader inReader = null;
        StringWriter outFileWriter = null;

        try {

            String template = getTemplate();
            inReader = new BufferedReader(new InputStreamReader(new FileInputStream(_inputTemplateName)));

            final File outFile = File.createTempFile("Template", ".java");

            outFileWriter = new StringWriter();

            String str = null;

            while ((str = inReader.readLine()) != null) {
                lineNumber++;
                if (processLine(str, outFileWriter)) {
                    outFileWriter.write("out.write(\"\\n\");");
                }
                outFileWriter.write("\n");
            }

            outFileWriter.flush();
            String code = outFileWriter.toString();


            String className = outFile.getName().replace(".java", "");
            template = template.replace("%NAME%", className);
            template = template.replace("%IMPORTS%", _imports.toString());
            template = template.replace("%CODE%", code);
            template = template.replace("%METHODS%", _methods.toString());

            template = template.replace("%FILENAME%", new File(_outputTemplateName).getAbsolutePath().replace("\\", "\\\\"));

            BufferedWriter javaCodeWriter = new BufferedWriter(new FileWriter(outFile.getAbsoluteFile()));
            javaCodeWriter.write(template);
            javaCodeWriter.close();


            String[] options = new String[]{
                    "-d", System.getProperty("java.io.tmpdir"),
                    outFile.getAbsolutePath()
            };

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                System.out.println("Compiler is null");
                return;
            }

            System.out.println("Using compiler: " + compiler.toString());
            DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<JavaFileObject>();
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnosticCollector, null, null);
            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromStrings(Arrays.asList(outFile.getAbsolutePath()));
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnosticCollector, null, null, compilationUnits);
            if (!task.call()) {
                System.out.println("Compilation failed");
                for (Diagnostic d : diagnosticCollector.getDiagnostics()) {
                    System.out.println(d.toString());
                }
                return;
            }

            URL classUrl = new File(outFile.getParent()).toURI().toURL();
            URLClassLoader classLoader = URLClassLoader.newInstance(
                    new URL[]{
                            classUrl
                    },
                    EntryPoint.class.getClassLoader()
            );


            Class<?> clazz = Class.forName(className, true, classLoader);
            Class<? extends Runnable> runClass = clazz.asSubclass(Runnable.class);
            Constructor<? extends Runnable> cTor = runClass.getConstructor();
            Runnable doRun = cTor.newInstance();
            doRun.run();

            File classFile = new File(outFile.getAbsolutePath().replace(".java", ".class"));
            classFile.deleteOnExit();
            if(_deleteJavaFile){
                outFile.deleteOnExit();
            }


        } catch (Exception e) {
            System.err.printf("Error on line: %d. ", lineNumber);
            e.printStackTrace(System.err);
        } finally {
        }
    }

}
