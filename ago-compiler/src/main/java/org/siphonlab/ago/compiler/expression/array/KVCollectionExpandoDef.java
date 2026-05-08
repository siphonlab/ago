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
package org.siphonlab.ago.compiler.expression.array;

import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.FunctionDef;
import org.siphonlab.ago.compiler.NullableClassDef;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.Expression;
import org.siphonlab.ago.compiler.expression.literal.ClassRefLiteral;
import org.siphonlab.ago.compiler.generic.GenericInstantiationClassDef;

public final class KVCollectionExpandoDef extends ObjectLiteralKVDef {
    private final FunctionDef ownerFunction;
    private Expression expression;
    private Kind kind;
    private SourceLocation sourceLocation;

    public enum Kind {
        ReadOnlyMap,
        KeyValuePairIterable,
        KeyValuePairIterator,
        Object
    }

    public Kind getKind() {
        return kind;
    }

    public KVCollectionExpandoDef(FunctionDef ownerFunction, Expression expression, SourceLocation sourceLocation) throws CompilationError {
        this.ownerFunction = ownerFunction;
        this.expression = expression;
        this.sourceLocation = sourceLocation;
        this.inferKeyValueType();
    }

    private void inferKeyValueType() throws CompilationError {
        var root = ownerFunction.getRoot();
        var t = this.expression.inferType();
        if(t instanceof NullableClassDef nullableClassDef){
            t = nullableClassDef.getNullableBaseClass();
        }
        var asReadonlyMap = (GenericInstantiationClassDef)root.getAnyReadonlyMap().asThatOrSuperOfThat(t);
        if(asReadonlyMap != null){
            ClassRefLiteral[] typeArgumentsArray = asReadonlyMap.getGenericSource().typeArguments();
            keyType = typeArgumentsArray[0].getClassDefValue();
            valueType = typeArgumentsArray[1].getClassDefValue();
            this.kind = Kind.ReadOnlyMap;
            return;
        }
        var iterable = root.getAnyIterableInterface().asThatOrSuperOfThat(t);
        if(iterable != null){
            ClassRefLiteral[] typeArgumentsArray = iterable.getGenericSource().typeArguments();
            var kvpair = (GenericInstantiationClassDef)root.getAnyKeyValuePairClass().asThatOrSuperOfThat(typeArgumentsArray[0].getClassDefValue());
            if(kvpair != null){
                typeArgumentsArray = kvpair.getGenericSource().typeArguments();
                keyType = typeArgumentsArray[0].getClassDefValue();
                valueType = typeArgumentsArray[1].getClassDefValue();
                this.kind = Kind.KeyValuePairIterable;
                return;
            } else {
                throw new TypeMismatchError("Iterable<KeyValuePair<>> expected", expression.getSourceLocation());
            }
        }

        var iterator = root.getAnyIteratorInterface().asThatOrSuperOfThat(t);
        if(iterator != null){
            ClassRefLiteral[] typeArgumentsArray = iterator.getGenericSource().typeArguments();
            var kvpair = (GenericInstantiationClassDef)root.getAnyKeyValuePairClass().asThatOrSuperOfThat(typeArgumentsArray[0].getClassDefValue());
            if(kvpair != null){
                typeArgumentsArray = kvpair.getGenericSource().typeArguments();
                keyType = typeArgumentsArray[0].getClassDefValue();
                valueType = typeArgumentsArray[1].getClassDefValue();
                this.kind = Kind.KeyValuePairIterator;
                return;
            } else {
                throw new TypeMismatchError("Iterator<KeyValuePair<>> expected", expression.getSourceLocation());
            }
        }

        keyType = root.STRING();
        valueType = root.getObjectClass();
        this.kind = Kind.Object;
    }


    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    @Override
    public String toString() {
        return "..." + expression;
    }

    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

}
