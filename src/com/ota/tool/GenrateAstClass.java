package com.ota.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class GenrateAstClass {
    public static void main(String args[]) throws IOException {
        if(args.length != 1) {
            System.out.println("Usage: generate_ast <output_directory>");
            System.exit(-1);
        }
        String outputdir = args[0];
        defineAst(outputdir, "Expr", Arrays.asList(
			"Assign : Token name, Expr value",
			"Binary   : Expr left, Token operator, Expr right",
			"Grouping : Expr expression",
			"Literal  : Object value",
			"Logical  : Expr left, Token operator, Expr right",
			"Unary    : Token operator, Expr right",
			"Variable : Token name"
        ));

        defineAst(outputdir, "Stmt", Arrays.asList(
			"Block : List<Stmt> statements",
			"Expression : Expr expression",
			"If         : Expr condition, Stmt thenBranch, Stmt elseBranch",
			"Print      : Expr expression",
			"Var        : Token name, Expr initializer",
			"While      : Expr condition, Stmt body"
        ));
    }

    private static void defineAst(String outdir, String base, List<String> types) throws IOException {
        String path = outdir + "/" + base + ".java";
        PrintWriter writer = new PrintWriter(path, "UTF-8");

        writer.println("package com.ota.jlox;\n");
        if(base.equals("Stmt")) {
        	writer.println("import java.util.List;\n");
        }
        writer.println("abstract class " + base + " {\n");

        defineVisitor(writer, base, types);

        for(String type : types) {
            String subclass = type.split(":")[0].trim();
            String fields = type.split(":")[1].trim();
            defineSubClass(writer, base, subclass, fields);
        }

        writer.println();
        writer.println("\tabstract <R> R accept(Visitor<R> visitor);");

        writer.println("}");
        writer.close();
    }

    private static void defineSubClass(PrintWriter writer, String base, String sub, String fields) {
        writer.println("\tstatic class " + sub + " extends " + base + " {\n");

        writer.println("\t\t" + sub + "(" + fields + ") {");
        for(String field : fields.split(", ")) {
            String name = field.split(" ")[1];
            writer.println("\t\t\tthis." + name + " = " + name + ";");
        }
        writer.println("\t\t}\n");

        writer.println("\t\t@Override");
        writer.println("\t\t<R> R accept(Visitor<R> visitor) {");
        writer.println("\t\t\treturn visitor.visit" + sub + base + "(this);");
        writer.println("\t\t}\n");

        for(String field : fields.split(", ")) {
            writer.println("\t\tfinal " + field + ";");
        }
        writer.println("\t}\n");
    }

    private static void defineVisitor(PrintWriter writer, String base, List<String> types) throws IOException {
        writer.println("\tinterface Visitor<R> {");
        for(String type : types) {
            String name = type.split(":")[0].trim();
            writer.println("\t\tR visit" + name + base + "(" + name + " " + base.toLowerCase() + ");");
        }
        writer.println("\t}\n\n");
    }
}
