/*
 * Copyright © 2026 Inshua (inshua@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.siphonlab.ago;

public class Modifier {
    public static String toString(int modifiers){
        StringBuilder sb = new StringBuilder();
        if((modifiers & AgoClass.PUBLIC) == AgoClass.PUBLIC)  sb.append("public ");
        if((modifiers & AgoClass.PRIVATE) == AgoClass.PRIVATE)  sb.append("private ");
        if((modifiers & AgoClass.PROTECTED) == AgoClass.PROTECTED)  sb.append("protected ");
        if((modifiers & AgoClass.STATIC) == AgoClass.STATIC)  sb.append("static ");
        if((modifiers & AgoClass.FINAL) == AgoClass.FINAL)  sb.append("final ");
        if((modifiers & AgoClass.SYNCHRONIZED) == AgoClass.SYNCHRONIZED)  sb.append("synchronized ");
        if((modifiers & AgoClass.VOLATILE) == AgoClass.VOLATILE)  sb.append("volatile ");
        if((modifiers & AgoClass.TRANSIENT) == AgoClass.TRANSIENT)  sb.append("transient ");
        if((modifiers & AgoClass.NATIVE) == AgoClass.NATIVE)  sb.append("native ");
        if((modifiers & AgoClass.INTERFACE) == AgoClass.INTERFACE)  sb.append("interface ");
        if((modifiers & AgoClass.ABSTRACT) == AgoClass.ABSTRACT)  sb.append("abstract ");
        if((modifiers & AgoClass.STRICT) == AgoClass.STRICT)  sb.append("strict ");
        if((modifiers & AgoClass.PARAMETER) == AgoClass.PARAMETER)  sb.append("parameter ");
        if((modifiers & AgoClass.FIELD_PARAM) == AgoClass.FIELD_PARAM)  sb.append("field_param ");
        if((modifiers & AgoClass.THIS_PARAM) == AgoClass.THIS_PARAM)  sb.append("this_param ");
        if((modifiers & AgoClass.OVERRIDE) == AgoClass.OVERRIDE)  sb.append("override ");
        if((modifiers & AgoClass.GETTER) == AgoClass.GETTER)  sb.append("getter ");
        if((modifiers & AgoClass.SETTER) == AgoClass.SETTER)  sb.append("setter ");
        if((modifiers & AgoClass.EMPTY_METHOD) == AgoClass.EMPTY_METHOD)  sb.append("empty_method ");
        if((modifiers & AgoClass.CONSTRUCTOR) == AgoClass.CONSTRUCTOR)  sb.append("constructor ");
        if((modifiers & AgoClass.VAR_ARGS) == AgoClass.VAR_ARGS)  sb.append("var_args ");
        if((modifiers & AgoClass.GENERIC_TEMPLATE) == AgoClass.GENERIC_TEMPLATE)  sb.append("generic_template ");
        if((modifiers & AgoClass.GENERIC_INSTANTIATION) == AgoClass.GENERIC_INSTANTIATION)  sb.append("generic_instantiation ");
        if(!sb.isEmpty()) sb.setLength(sb.length() - 1);
        return sb.toString();
    }
}
