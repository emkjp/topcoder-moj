package moj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.topcoder.client.contestant.ProblemComponentModel;
import com.topcoder.shared.language.Language;
import com.topcoder.shared.problem.*;

public class CSharpHarnessGenerator implements HarnessGenerator {
    final ProblemComponentModel m_problem;
    final Language				m_lang;
    final Preferences           m_pref;

    public CSharpHarnessGenerator(ProblemComponentModel problem, Language lang, Preferences pref) {
        m_problem = problem;
        m_lang = lang;
        m_pref = pref;
    }

    public String generateDefaultMain() {
        return
                "public static void Main(string[] args) {\n" +
                "\t\tif (args.Length == 0) {\n" +
                "\t\t\t" + m_problem.getClassName() + "Harness.run_test(-1);\n" + 
                "\t\t} else {\n" +
                "\t\t\tfor (int i=0; i<args.Length; ++i)\n" +
                "\t\t\t\t" + m_problem.getClassName() + "Harness.run_test(Int32.Parse(args[i]));\n" +
                "\t\t}\n" +
                "\t}";
    }

    public String generateRunTest() {
        return m_problem.getClassName() + "Harness.run_test(-1);";
    }

    void generateNamespaceStart(ArrayList<String> code) {
        code.add("class " + m_problem.getClassName() + "Harness {");
    }

    void generateRunTest(ArrayList<String> code) {
        code.add("   public static void run_test(int casenum) {");

        code.add("      if (casenum != -1) {");
        code.add("         if (runTestCase(casenum) == -1)");
        code.add("            System.Console.Error.WriteLine(\"Illegal input! Test case \" + casenum + \" does not exist.\");");
        code.add("         return;");
        code.add("      }");
        code.add("      ");
        code.add("      int correct = 0, total = 0;");
        code.add("      for (int i=0;; ++i) {");
        code.add("         int x = runTestCase(i);");
        code.add("         if (x == -1) {");
        code.add("            if (i >= 100) break;");
        code.add("            continue;");
        code.add("         }");
        code.add("         correct += x;");
        code.add("         ++total;");
        code.add("      }");
        code.add("      ");
        code.add("      if (total == 0) {");
        code.add("         System.Console.Error.WriteLine(\"No test cases run.\");");
        code.add("      } else if (correct < total) {");
        code.add("         System.Console.Error.WriteLine(\"Some cases FAILED (passed \" + correct + \" of \" + total + \").\");");
        code.add("      } else {");
        code.add("         System.Console.Error.WriteLine(\"All \" + total + \" tests passed!\");");
        code.add("      }");
        code.add("   }");
        code.add("   ");
    }

    void generateOutputComparison(ArrayList<String> code) {
        DataType returnType = m_problem.getReturnType();
        if (returnType.getBaseName().equals("double")) {
            code.add("   const double MAX_DOUBLE_ERROR = 1E-9;");
            code.add("   static bool compareOutput(double expected, double result){ if(Double.IsNaN(expected)){ return Double.IsNaN(result); }else if(Double.IsInfinity(expected)){ if(expected > 0){ return result > 0 && Double.IsInfinity(result); }else{ return result < 0 && Double.IsInfinity(result); } }else if(Double.IsNaN(result) || Double.IsInfinity(result)){ return false; }else if(Math.Abs(result - expected) < MAX_DOUBLE_ERROR){ return true; }else{ double min = Math.Min(expected * (1.0 - MAX_DOUBLE_ERROR), expected * (1.0 + MAX_DOUBLE_ERROR)); double max = Math.Max(expected * (1.0 - MAX_DOUBLE_ERROR), expected * (1.0 + MAX_DOUBLE_ERROR)); return result > min && result < max; } }");
            code.add("   static double relativeError(double expected, double result) { if (Double.IsNaN(expected) || Double.IsInfinity(expected) || Double.IsNaN(result) || Double.IsInfinity(result) || expected == 0) return 0; return Math.Abs(result-expected) / Math.Abs(expected); }");
            if (returnType.getDimension() > 0) {
                code.add("   static bool compareOutput(double[] expected, double[] result) { if (expected.Length != result.Length) return false; for (int i=0; i<expected.Length; ++i) if (!compareOutput(expected[i], result[i])) return false; return true; }");
                code.add("   static double relativeError(double[] expected, double[] result) { double ret = 0.0; for (int i=0; i<expected.Length; ++i) { ret = Math.Max(ret, relativeError(expected[i], result[i])); } return ret; }");
            }
            code.add("   ");
        } else if (returnType.getBaseName().equals("String")) {
            if (returnType.getDimension() > 0) {
                code.add("   static bool compareOutput(String[] expected, String[] result) { if (expected.Length != result.Length) return false; for (int i=0; i<expected.Length; ++i) if (expected[i] != result[i]) return false; return true; }\n");
            } else {
                code.add("   static bool compareOutput(String expected, String result) { return expected == result; }");				
            }
        } else {
            String type = returnType.getBaseName();
            if (returnType.getDimension() > 0) {
                code.add("   static bool compareOutput("+type+"[] expected, "+type+"[] result) { if (expected.Length != result.Length) return false; for (int i=0; i<expected.Length; ++i) if (expected[i] != result[i]) return false; return true; }\n");
            } else {
                code.add("   static bool compareOutput("+type+" expected, "+type+" result) { return expected == result; }");				
            }
        }
    }

    void generateFormatResult(ArrayList<String> code) {
        DataType returnType = m_problem.getReturnType();

        Map<String, String> typeFormatMap = new HashMap<String, String>();
        typeFormatMap.put("int", "{0}");
        typeFormatMap.put("float", "{0:g10}");
        typeFormatMap.put("char", "'{0}'");
        typeFormatMap.put("byte", "{0}");
        typeFormatMap.put("short", "{0}");
        typeFormatMap.put("long", "{0}");
        typeFormatMap.put("double", "{0:g10}");
        typeFormatMap.put("String", "\\\"{0}\\\"");
        typeFormatMap.put("bool", "{0}");
        String formatString = typeFormatMap.get(returnType.getBaseName());

        code.add("   static String formatResult(" + returnType.getDescriptor(m_lang) + " res) {");
        if (returnType.getDimension() > 0) {
            code.add("      String ret = \"\";");
            code.add("      ret += \"{\";");
            code.add("      for (int i=0; i<res.Length; ++i) {");
            code.add("         if (i > 0) ret += \",\";");
            code.add("         ret += String.Format(\" " + formatString + "\", res[i]);");
            code.add("      }");
            code.add("      ret += \" }\";");
            code.add("      return ret;");
        } else {
            code.add("      return String.Format(\"" + formatString + "\", res);");
        }

        code.add("   }");
        code.add("   ");
    }

    void generateVerifyCase(ArrayList<String> code) {
        DataType returnType = m_problem.getReturnType();
        String typeName = returnType.getDescriptor(m_lang);

        code.add("   static int verifyCase(int casenum, " + typeName + " expected, " + typeName + " received) { ");
        code.add("      System.Console.Error.Write(\"Example \" + casenum + \"... \");");

        // Print "PASSED" or "FAILED" based on the result
        if (returnType.getBaseName().equals("double")) {
            code.add("      if (compareOutput(expected, received)) {");
            code.add("         System.Console.Error.Write(\"PASSED\");");
            code.add("         double rerr = relativeError(expected, received);");
            code.add("         if (rerr > 0) System.Console.Error.Write(\" (relative error {0:g})\", rerr);");
            code.add("         System.Console.Error.WriteLine();");
            code.add("         return 1;");
        } else {
            code.add("      if (compareOutput(expected, received)) {");
            code.add("         System.Console.Error.WriteLine(\"PASSED\");");
            code.add("         return 1;");
        }
        code.add("      } else {");
        code.add("         System.Console.Error.WriteLine(\"FAILED\");");

        code.add("         System.Console.Error.WriteLine(\"    Expected: \" + formatResult(expected)); ");
        code.add("         System.Console.Error.WriteLine(\"    Received: \" + formatResult(received)); ");

        code.add("         return 0;");
        code.add("      }");
        code.add("   }");
        code.add("");
    }

    void generateParameter(ArrayList<String> code, DataType paramType, String name, String contents, boolean isPlaceholder) {
        if (isPlaceholder)
            contents = "";

        String baseName = paramType.getBaseName();
        boolean isLong = baseName.equals("long");
        String typeName = paramType.getDescriptor(m_lang) + " " + name;
        if (isLong) {
            if (paramType.getDimension() == 0) {
                contents = ConstantFormatting.formatLongForJava(contents);
            } else {
                contents = ConstantFormatting.formatLongArrayForJava(contents);
            }
        }

        while (typeName.length() < 25)
            typeName = typeName + " ";

        if (!baseName.equals("String")) {
            // Compress spaces in non-strings
            contents = contents.replaceAll("\\s+", " "); 
        }

        code.add("         " + typeName + " = " + contents + ";");
    }

    void generateTestCase(ArrayList<String> code, int index, TestCase testCase, boolean isPlaceholder) {
        DataType[] paramTypes = m_problem.getParamTypes();
        String[] paramNames = m_problem.getParamNames();
        DataType returnType = m_problem.getReturnType();

        String[] inputs = testCase.getInput();
        String output = testCase.getOutput();

        /*
         * Generate code for setting up individual test cases
         * and calling the method with these parameters.
         */
        // Generate each input variable separately
        for (int i = 0; i < inputs.length; ++i) {
            generateParameter(code, paramTypes[i], paramNames[i], inputs[i], isPlaceholder);
        }

        // Generate the output variable as the last variable
        generateParameter(code, returnType, "expected__", output, isPlaceholder);

        code.add("");

        StringBuffer line = new StringBuffer();
        line.append("         return verifyCase(casenum__, expected__, new " + m_problem.getClassName() + "()." + m_problem.getMethodName() + "(");

        // Generate the function call list
        for (int i = 0; i < inputs.length; ++i) {
            line.append(paramNames[i]);
            if (i < (inputs.length - 1))
                line.append(", ");
        }

        line.append("));");
        code.add(line.toString());
    }

    void generateRunTestCase(ArrayList<String> code) {
        TestCase[] testCases = m_problem.getTestCases();

        code.add("   static int runTestCase(int casenum__) {");
        code.add("      switch(casenum__) {");
        // Generate the individual test cases
        for (int i = 0; i < testCases.length+m_pref.getNumPlaceholders(); ++i) {
            if (i == testCases.length) {
                code.add("");
                code.add("      // custom cases");
                code.add("");
            }
            code.add((i >= testCases.length ? "/*" : "") + "      case " + i + ": {");
            generateTestCase(code, i, testCases[i < testCases.length ? i : 0], i >= testCases.length);
            code.add("      }" + (i >= testCases.length ? "*/" : ""));
        }

        // next
        code.add("      default:");
        code.add("         return -1;");
        code.add("      }");
        code.add("   }");
    }

    public String generateTestCode() {
        ArrayList<String> code = new ArrayList<String>();

        generateNamespaceStart(code);
        generateRunTest(code);

        generateOutputComparison(code);
        generateFormatResult(code);
        generateVerifyCase(code);
        generateRunTestCase(code);
        code.add("}");

        StringBuffer sb = new StringBuffer();
        for (String s : code) {
            sb.append(s);
            sb.append('\n');
        }
        String ret = sb.toString();
        ret = Pattern.compile("^               ", Pattern.MULTILINE).matcher(ret).replaceAll("\t\t\t\t\t");
        ret = Pattern.compile("^            "   , Pattern.MULTILINE).matcher(ret).replaceAll("\t\t\t\t");
        ret = Pattern.compile("^         "      , Pattern.MULTILINE).matcher(ret).replaceAll("\t\t\t");
        ret = Pattern.compile("^      "         , Pattern.MULTILINE).matcher(ret).replaceAll("\t\t");
        ret = Pattern.compile("^   "            , Pattern.MULTILINE).matcher(ret).replaceAll("\t");
        return ret;
    }
}
