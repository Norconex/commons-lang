package com.norconex.commons.lang.javadoc;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.sun.source.doctree.DocTree;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class IncludeDirective {
    private final String reference;
    private final String tagName;
    private final String className;
    private final String parseError;

    static IncludeDirective of(DocTree directive) {
        var tag = TagletUtil.toUnknownInlineTagTreeOrFail(
                directive, IncludeTaglet.NAME);

        // at this point, There shall be only one TEXT entry in content.
        return of(tag.getContent().get(0).toString());
    }

    static IncludeDirective of(String directive) {
        var b = IncludeDirective.builder();

        // className
        b.className(extractGroup1(directive, "^(.*?)(\\s|\\@|\\#|$)"));
        if (StringUtils.isBlank(b.className)) {
            b.parseError("Include directive missing fully qualified "
                    + "class name.");
            return b.build();
        }

        // tagName
        b.tagName(extractGroup1(directive, "@(.*?)(\\s|\\#|$)"));

        // reference
        b.reference(extractGroup1(directive, "#(.*?)(\\s|\\@|$)"));

        if (StringUtils.isAllBlank(b.tagName, b.reference)) {
            b.parseError("Include directive missing both tag name "
                    + "and reference.");
        }
        return b.build();
    }

    boolean matches(TagContent tag) {
        return !hasParseError() && nameOK(tag) && refOK(tag);
    }
    boolean hasParseError() {
        return parseError != null;
    }

    private boolean nameOK(TagContent tag) {
        return tagName == null || tagName.equals(tag.getName());
    }
    private boolean refOK(TagContent tag) {
        return reference == null || reference.equals(tag.getReference());
    }
    private static String extractGroup1(String txt, String regex) {
        var m = Pattern.compile(regex).matcher(txt);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }
}