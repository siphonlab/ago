package org.siphonlab.ago.compiler.generic;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.ConstructorDef;
import org.siphonlab.ago.compiler.MetaClassDef;
import org.siphonlab.ago.compiler.PhantomMetaClassDef;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.Literal;

public class ScopedClassIntervalClassDef extends ClassIntervalClassDef {

    public ScopedClassIntervalClassDef(ClassDef baseClass, ConstructorDef parameterizedConstructor, Literal<?>[] arguments) {
        super(baseClass, parameterizedConstructor, arguments);
    }

    @Override
    public ClassDef cloneForInstantiate(InstantiationArguments instantiationArguments, MutableBoolean returnExisted) {
        ScopedClassIntervalClassDef c = null;
        try {
            c = this.getParentClass().getOrCreateScopedClassInterval(baseClass.instantiate(instantiationArguments, returnExisted), constructor, mapArguments(instantiationArguments), returnExisted);
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
        return c;
    }

    public static ClassDef getLBoundClass(ClassIntervalClassDef classIntervalClassDef){
        return classIntervalClassDef.getLBoundClass();
    }

    public static MetaClassDef getMetaOfLBoundClass(ClassIntervalClassDef classIntervalClassDef) {
        var lBound = classIntervalClassDef.getLBoundClass();
        MetaClassDef metaClassDef = lBound.getMetaClassDef();
        if(metaClassDef == null){
            return new PhantomMetaClassDef(lBound);
        }
        return metaClassDef;
    }
}
