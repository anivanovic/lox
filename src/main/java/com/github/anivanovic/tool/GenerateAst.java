package com.github.anivanovic.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.List;

public class GenerateAst {

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.err.println("Usage: generate_ast <output_dir>");
      System.exit(64);
    }
    String outDir = args[0];
    defineAst(outDir, "Expr", List.of(
            "Binary     : Expr left, Token operator, Expr right",
            "Grouping   : Expr expression",
            "Literal    : Object value",
            "Unary      : Token operator, Expr right"
    ));
  }

  private static void defineAst(String outDir, String baseName, List<String> types) throws IOException {
    String path = Paths.get(outDir, baseName+".java").toString();
    PrintWriter writer = new PrintWriter(path, Charset.forName("UTF-8"));
    writer.println("package com.github.anivanovic.jezik;");
    writer.println();
    writer.println("import java.util.List;");
    writer.println();
    writer.println("abstract class " + baseName + " {");

    defineVisitor(writer, baseName, types);

    // Create AST classes
    for (String def : types) {
      String className = def.split(":")[0].trim();
      String fields = def.split(":")[1].trim();
      defineType(writer, baseName, className, fields);
    }
    writer.println();
    writer.println("  abstract <R> R accept(Visitor<R> visitor);");
    writer.println("}");
    writer.close();
  }

  private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
    writer.println("  interface Visitor<R> {");
    
    for (String type : types) {
        String typeName = type.split(":")[0].trim();
      writer.println(
          "    R visit"
              + typeName
              + baseName
              + "("
              + typeName
              + " "
              + baseName.toLowerCase()
              + ");");
    }
    writer.println("  }");
  }

  private static void defineType(PrintWriter writer, String baseName, String className, String fieldList) {
    writer.println("  static class " + className + " extends " + baseName + " {");
    writer.println();

    // Fields
    String[] fields = fieldList.split(", ");
    for (String field : fields) {
      writer.println("    final " + field + ";");
    }
    writer.println();

    // Constructor
    writer.println("    " + className + "(" + fieldList + ") {");
    for (String field : fields) {
      String name = field.split(" ")[1];
      writer.println("      this." + name + " = " + name + ";");
    }
    writer.println("    }");

    // Expression method impl
    writer.println();
    writer.println("    @Override");
    writer.println("    <R> R accept(Visitor<R> visitor) {");
    writer.println("      return visitor.visit" + className + baseName + "(this);");
    writer.println("    }");

    writer.println("  }");
    writer.println();
  }
}
