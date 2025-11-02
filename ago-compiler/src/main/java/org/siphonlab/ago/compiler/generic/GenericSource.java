package org.siphonlab.ago.compiler.generic;

import org.siphonlab.ago.compiler.ClassDef;

/// `originalTemplate + arguments = instantiation class`.
public record GenericSource(ClassDef originalTemplate, InstantiationArguments instantiationArguments) {

}
