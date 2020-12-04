package com.odm.setupwizardoverlay.data;

import java.io.Serializable;
import java.util.List;

/**
 * Created by pecuyu on 20-3-3.
 */

public class VzwSetupJsonData {
    String error;  // Error object with error code.  If this attribute is present OEM will default to error case.
    String type;  // such as native_ui
    List<VzwSetupPage> components; // pages of the data

    public List<VzwSetupPage> getComponents() {
        return components;
    }

    public String getError() {
        return error;
    }

    public String getType() {
        return type;
    }

    public static class VzwSetupPage implements Serializable {
        String pageHeader;
        List<Element> elements;

        public static class Element implements Serializable{
            String type;
            int pageIndex;  // only valid for navButton
            String content;
            String label;  // for description of the view, such as checkbox
            String header;   // the header of the content
            List<String> items; // for group views, such as radio_group
            String value; // for status of the specific view, such as checkbox,radio_group,toggle
            String id; // for checkbox, radio_group


            @Override
            public String toString() {
                return "Element{" +
                        "type='" + type + '\'' +
                        ", pageIndex=" + pageIndex +
                        ", content='" + content + '\'' +
                        ", label='" + label + '\'' +
                        ", header='" + header + '\'' +
                        ", items=" + items +
                        ", value='" + value + '\'' +
                        ", id='" + id + '\'' +
                        '}';
            }

            public String getType() {
                return type;
            }

            public int getPageIndex() {
                return pageIndex;
            }

            public String getContent() {
                return content;
            }

            public String getLabel() {
                return label;
            }

            public String getHeader() {
                return header;
            }

            public List<String> getItems() {
                return items;
            }

            public String getValue() {
                return value;
            }

            public String getId() {
                return id;
            }

            public interface Type {
                String TYPE_HEADER = "header";
                String TYPE_LINE_BREAK = "line_break";
                String TYPE_TEXT = "text";
                String TYPE_NAV_BUTTON = "navButton";
                String TYPE_CHECKBOX = "checkbox";
                String TYPE_RADIO_GROUP = "radio_group";
                String TYPE_TOGGLE = "toggle";
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("pageHeader=" + pageHeader);
            sb.append("[");
            for (Element e : elements) {
                String newLine = String.format("type=%s,pageIndex=%d,content=%s,header=%s,items=%s,value=%s,id=%s,label=%s\n",
                        e.type, e.pageIndex, e.content, e.header, e.items, e.value, e.id, e.label);
                sb.append(newLine);
            }
            sb.append("]");
            return sb.toString();
        }

        public String getPageHeader() {
            return pageHeader;
        }

        public List<Element> getElements() {
            return elements;
        }

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("error=").append(error).append(" ");
        sb.append("type=").append(type);
        sb.append("\n");
        for (VzwSetupPage page : components) {
            sb.append(page.toString());
        }
        return sb.toString();
    }


}
