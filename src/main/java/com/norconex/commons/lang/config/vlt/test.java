package com.norconex.commons.lang.config.vlt;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;

import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class test {

    public static void main(String[] args) {

        // Initialize Velocity engine
        VelocityEngine velocityEngine = new VelocityEngine();
        Properties props = new Properties();
        //        props.setProperty("file.resource.loader.path", "src/main/resources"); // Template path
        props.setProperty("file.resource.loader.path",
                "src/test/resources/config/vlt"); // Template path
        props.setProperty("userdirective",
                "com.norconex.commons.lang.config.vlt.CustomIncludeDirective, com.norconex.commons.lang.config.vlt.CustomParseDirective");

        //        props.setProperty("space.gobbling", "structured");
        //        props.setProperty("space.gobbling", "none");
        //        props.setProperty("space.gobbling", "lines"); //default

        velocityEngine.init(props);

        // Get the main template
        Template template = velocityEngine.getTemplate("main_template.vm");

        // Create the context and add data
        VelocityContext context = new VelocityContext();
        context.put("title", "template title");
        context.put("date", "2024-08-20");

        // Create a list of items
        //        List<Map<String, String>> items = new ArrayList<>();
        //        Map<String, String> item1 = new HashMap<>();
        //        item1.put("name", "Item1");
        //        item1.put("value", "Value1");
        //
        //        Map<String, String> item2 = new HashMap<>();
        //        item2.put("name", "Item2");
        //        item2.put("value", "Value2");
        //
        //        items.add(item1);
        //        items.add(item2);
        //
        //        context.put("items", items);

        // Merge template with context data
        StringWriter writer = new StringWriter();
        template.merge(context, writer);

        String templateContent = "";
        try {
            templateContent = new String(Files.readAllBytes(Paths
                    .get("src/test/resources/config/vlt/main_template.vm")));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Writer to hold the output of evaluating the file content as a string
        StringWriter fileContentOutputWriter = new StringWriter();

        // Evaluate the content of the .vm file using VelocityEngine.evaluate()
        try {
            velocityEngine.evaluate(context, fileContentOutputWriter, "logTag",
                    templateContent);
            System.out.println(fileContentOutputWriter.toString());

        } catch (ParseErrorException e) {
            System.err.println("Template parsing error: " + e.getMessage());
        } catch (MethodInvocationException e) {
            System.err.println("Method invocation error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("General error: " + e.getMessage());
        }

    }

}
