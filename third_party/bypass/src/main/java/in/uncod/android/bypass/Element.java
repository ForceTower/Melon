/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package in.uncod.android.bypass;

import java.util.Map;
import java.util.HashMap;

public class Element {

    public static final int F_LIST_ORDERED = 1;
    String text;
    Map<String, String> attributes = new HashMap<String, String>();
    Element[] children;
    Type type;
    Element parent;
    int nestLevel = 0;
    public Element(String text, int type) {
        this.text = text;
        this.type = Type.fromInteger(type);
    }

    public void setChildren(Element[] children) {
        this.children = children;
    }

    public void addAttribute(String name, String value) {
        attributes.put(name, value);
    }

    public String getAttribute(String name) {
        return attributes.get(name);
    }

    public Element getParent() {
        return parent;
    }

    public void setParent(Element element) {
        this.parent = element;
    }

    public String getText() {
        return text;
    }

    public int size() {
        if (children != null) {
            return children.length;
        }
        return 0;
    }

    public Type getType() {
        return type;
    }

    public boolean isBlockElement() {
        return (type.value & 0x100) == 0x000;
    }

    public boolean isSpanElement() {
        return (type.value & 0x100) == 0x100;
    }

    public enum Type {

        // Block Element Types

        BLOCK_CODE(0x000),
        BLOCK_QUOTE(0x001),
        BLOCK_HTML(0x002),
        HEADER(0x003),
        HRULE(0x004),
        LIST(0x005),
        LIST_ITEM(0x006),
        PARAGRAPH(0x007),
        TABLE(0x008),
        TABLE_CELL(0x009),
        TABLE_ROW(0x00A),

        // Span Element Types

        AUTOLINK(0x10B),
        CODE_SPAN(0x10C),
        DOUBLE_EMPHASIS(0x10D),
        EMPHASIS(0x10E),
        IMAGE(0x10F),
        LINEBREAK(0x110),
        LINK(0x111),
        RAW_HTML_TAG(0x112),
        TRIPLE_EMPHASIS(0x113),
        TEXT(0x114),
        STRIKETHROUGH(0x115);

        private static final Type[] TypeValues = Type.values();
        private final int value;

        private Type(int value) {
            this.value = value;
        }

        public static Type fromInteger(int x) {
            for (Type type : TypeValues) {
                if (type.value == x) {
                    return type;
                }
            }
            return null;
        }
    }
}
