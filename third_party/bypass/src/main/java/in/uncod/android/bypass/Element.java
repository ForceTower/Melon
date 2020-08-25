/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2020. João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
