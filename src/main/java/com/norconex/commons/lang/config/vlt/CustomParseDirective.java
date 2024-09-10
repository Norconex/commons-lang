package com.norconex.commons.lang.config.vlt;

import org.apache.velocity.Template;
import org.apache.velocity.app.event.EventHandlerUtil;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.directive.Parse;
import org.apache.velocity.runtime.directive.StopCommand;
import org.apache.velocity.runtime.parser.node.ASTDirective;
import org.apache.velocity.runtime.parser.node.Node;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.apache.velocity.util.StringUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class CustomParseDirective extends Parse {

    @Override
    public boolean render(InternalContextAdapter context,
            Writer writer, Node node)
            throws IOException, ResourceNotFoundException, ParseErrorException,
            MethodInvocationException {

        int maxDepth;
        try {
            Field maxDepthField = Parse.class.getDeclaredField("maxDepth");

            maxDepthField.setAccessible(true);
            maxDepth = (int) maxDepthField.get(this); // Cast to int

        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        if (node.jjtGetNumChildren() == 0) {
            throw new VelocityException("#parse(): argument missing at " +
                    StringUtils.formatFileString(this), null,
                    rsvc.getLogContext().getStackTrace());
        }

        Object value = node.jjtGetChild(0).value(context);
        if (value == null) {
            log.debug("#parse(): null argument at {}",
                    StringUtils.formatFileString(this));
        }

        String sourcearg = value == null ? null : value.toString();

        String arg = EventHandlerUtil.includeEvent(rsvc, context, sourcearg,
                context.getCurrentTemplateName(), getName());

        if (strictRef && value == null && arg == null) {
            throw new VelocityException(
                    "The argument to #parse returned null at "
                            + StringUtils.formatFileString(this),
                    null, rsvc.getLogContext().getStackTrace());
        }

        if (arg == null) {
            return true;
        }

        if (maxDepth > 0) {
            String[] templateStack = context.getTemplateNameStack();
            if (templateStack.length >= maxDepth) {
                StringBuilder path = new StringBuilder();
                for (String aTemplateStack : templateStack) {
                    path.append(" > ").append(aTemplateStack);
                }
                log.error("Max recursion depth reached ({}). File stack: {}",
                        templateStack.length, path);

                return false;
            }
        }

        Template t = null;

        try {
            t = rsvc.getTemplate(arg, getInputEncoding(context));
        } catch (ResourceNotFoundException rnfe) {
            log.error("#parse(): cannot find template '{}', called at {}",
                    arg, StringUtils.formatFileString(this));
            throw rnfe;
        } catch (ParseErrorException pee) {
            log.error(
                    "#parse(): syntax error in #parse()-ed template '{}', called at {}",
                    arg, StringUtils.formatFileString(this));
            throw pee;
        } catch (RuntimeException e) {
            log.error("Exception rendering #parse({}) at {}",
                    arg, StringUtils.formatFileString(this));
            throw e;
        } catch (Exception e) {
            String msg = "Exception rendering #parse(" + arg + ") at " +
                    StringUtils.formatFileString(this);
            log.error(msg, e);
            throw new VelocityException(msg, e,
                    rsvc.getLogContext().getStackTrace());
        }
        List<Template> macroLibraries = context.getMacroLibraries();

        if (macroLibraries == null) {
            macroLibraries = new ArrayList<>();
        }

        context.setMacroLibraries(macroLibraries);

        macroLibraries.add(t);

        try {
            preRender(context);
            context.pushCurrentTemplateName(arg);

            //Mod start
            Writer placeHolder = new StringWriter();
            ((SimpleNode) t.getData()).render(context, placeHolder);

            String prefixSpace = ((ASTDirective) node).getPrefix();

            String prefixedContent =
                    (placeHolder.toString()).replaceAll("(?m)^", prefixSpace);
            writer.append(prefixedContent);
            //Mod end

        } catch (StopCommand stop) {
            if (!stop.isFor(this)) {
                throw stop;
            }
        } catch (RuntimeException e) {
            log.error("Exception rendering #parse({}) at {}",
                    arg, StringUtils.formatFileString(this));
            throw e;
        } catch (Exception e) {
            String msg = "Exception rendering #parse(" + arg + ") at " +
                    StringUtils.formatFileString(this);
            log.error(msg, e);
            throw new VelocityException(msg, e,
                    rsvc.getLogContext().getStackTrace());
        } finally {
            context.popCurrentTemplateName();
            postRender(context);
        }
        return true;
    }
}
