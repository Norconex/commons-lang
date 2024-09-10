package com.norconex.commons.lang.config.vlt;

import org.apache.velocity.app.event.EventHandlerUtil;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.directive.Include;
import org.apache.velocity.runtime.parser.node.ASTDirective;
import org.apache.velocity.runtime.parser.node.Node;
import org.apache.velocity.runtime.parser.node.ParserTreeConstants;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.util.StringUtils;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;

public class CustomIncludeDirective extends Include {
    @Override
    public boolean render(InternalContextAdapter context,
            Writer writer, Node node)
            throws IOException, MethodInvocationException,
            ResourceNotFoundException {
        try {
            // Obtain the protected method 'outputErrorToStream' from the superclass 'Include'
            Method outputErrorToStreamMethod = Include.class.getDeclaredMethod(
                    "outputErrorToStream",
                    Writer.class,
                    String.class);
            outputErrorToStreamMethod.setAccessible(true); // Make the protected method accessible

            int argCount = node.jjtGetNumChildren();

            for (int i = 0; i < argCount; i++) {
                Node n = node.jjtGetChild(i);

                if (n.getType() == ParserTreeConstants.JJTSTRINGLITERAL ||
                        n.getType() == ParserTreeConstants.JJTREFERENCE) {
                    if (!renderOutput(n, context, writer))
                        outputErrorToStreamMethod.invoke(this, writer,
                                "error with arg " + i + " please see log.");
                } else {
                    String msg = "invalid #include() argument '"
                            + n.toString() + "' at "
                            + StringUtils.formatFileString(this);
                    log.error(msg);
                    outputErrorToStreamMethod.invoke(this, writer,
                            "error with arg " + i + " please see log.");
                    throw new VelocityException(msg, null,
                            rsvc.getLogContext().getStackTrace());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private boolean renderOutput(Node node, InternalContextAdapter context,
            Writer writer)
            throws IOException, MethodInvocationException,
            ResourceNotFoundException {
        if (node == null) {
            log.error("#include() null argument");
            return false;
        }

        Object value = node.value(context);
        if (value == null) {
            log.error("#include() null argument");
            return false;
        }

        String sourcearg = value.toString();

        String arg = EventHandlerUtil.includeEvent(rsvc, context, sourcearg,
                context.getCurrentTemplateName(), getName());

        boolean blockinput = false;
        if (arg == null)
            blockinput = true;

        Resource resource = null;

        try {
            if (!blockinput)
                resource = rsvc.getContent(arg, getInputEncoding(context));
        } catch (ResourceNotFoundException rnfe) {
            log.error("#include(): cannot find resource '{}', called at {}",
                    arg, StringUtils.formatFileString(this));
            throw rnfe;
        }

        catch (RuntimeException e) {
            log.error("#include(): arg = '{}', called at {}",
                    arg, StringUtils.formatFileString(this));
            throw e;
        } catch (Exception e) {
            String msg = "#include(): arg = '" + arg +
                    "', called at " + StringUtils.formatFileString(this);
            log.error(msg, e);
            throw new VelocityException(msg, e,
                    rsvc.getLogContext().getStackTrace());
        }

        if (blockinput)
            return true;

        else if (resource == null)
            return false;

        String prefixSpace = ((ASTDirective) node.jjtGetParent()).getPrefix();
        String indentedData =
                ((String) resource.getData()).replaceAll("(?m)^", prefixSpace);
        writer.write(indentedData);

        return true;
    }
}
