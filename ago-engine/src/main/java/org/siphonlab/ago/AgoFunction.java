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


import org.siphonlab.ago.classloader.AgoClassLoader;

import java.util.HashMap;
import java.util.Map;

public class AgoFunction extends AgoClass{

    private TypeCode resultTypeCode;
    private AgoClass resultClass;

    private AgoVariable[] variables;
    private AgoParameter[] parameters;

    protected int[] code;     // compiled code, for class it's the initializers
    private SwitchTable[] switchTables;
    private TryCatchItem[] tryCatchItems;
    private SourceMapEntry[] sourceMap;

    Map<String, AgoParameter> parameterByName;


    public AgoFunction(AgoClassLoader classLoader,  MetaClass metaClass, String fullname, String name) {
        super(classLoader, metaClass, fullname, name);
        this.type = AgoClass.TYPE_FUNCTION;
    }

    public void setResultType(TypeCode typeCode, AgoClass agoClass) {
        this.resultTypeCode = typeCode;
        this.resultClass = agoClass;
    }

    public void setVariables(AgoVariable[] variables) {
        this.variables = variables;
    }

    public AgoVariable[] getVariables() {
        return variables;
    }

    public void setParameters(AgoParameter[] parameters) {
        this.parameters = parameters;
    }

    public AgoParameter[] getParameters() {
        return parameters;
    }

    public AgoClass getResultClass() {
        return resultClass;
    }

    public TypeCode getResultTypeCode() {
        return resultTypeCode;
    }

    public void setCode(int[] code) {
        this.code = code;
    }

    public int[] getCode() {
        return code;
    }

    public boolean isConstructor(){
        return (this.modifiers & AgoClass.CONSTRUCTOR) != 0;
    }

    public boolean isEmptyMethod(){
        return (this.modifiers & AgoClass.EMPTY_METHOD) != 0;
    }

    public void setSwitchTables(SwitchTable[] switchTables) {
        this.switchTables = switchTables;
    }

    public SwitchTable[] getSwitchTables() {
        return switchTables;
    }

    public void setTryCatchItems(TryCatchItem[] items) {
        this.tryCatchItems = items;
    }

    public TryCatchItem[] getTryCatchItems() {
        return tryCatchItems;
    }

    public void setSourceMap(SourceMapEntry[] sourceMap) {
        this.sourceMap = sourceMap;
    }

    public SourceMapEntry[] getSourceMap() {
        return sourceMap;
    }

    public AgoParameter findParameter(String name) {
        if (parameterByName == null) {
            parameterByName = new HashMap<>(parameters.length);
            for (AgoParameter field : this.parameters) {
                parameterByName.put(field.getName(), field);
            }
        }
        return parameterByName.get(name);
    }

    @Override
    public AgoFunction cloneWithScope(Instance<?> parentScope) {
        if (parentScope == this.parentScope) return this;
        var copy = new AgoFunction(this.getClassLoader(),this.agoClass,this.fullname,this.name);
        copy.setParentScope(parentScope);
        this.copyTo(copy);
        return copy;
    }

    @Override
    protected void copyTo(AgoClass cls) {
        AgoFunction copy = (AgoFunction) cls;
        super.copyTo(cls);
        copy.setResultType(resultTypeCode, resultClass);
        copy.setVariables(this.getVariables());
        copy.setParameters(this.getParameters());
        copy.setCode(this.getCode());
        copy.setSwitchTables(this.getSwitchTables());
        copy.setTryCatchItems(this.getTryCatchItems());
        copy.setSourceMap(this.getSourceMap());
        copy.parameterByName = this.parameterByName;
    }
}
