package org.siphonlab.ago.compiler;

import java.util.List;

/**
 * ConcreteType like array: int[], parameterized class: VarChar::(200), generic instantiation types: G&lt;Animal&gt;
 * in the compiling time, the concrete class (now they are Array, ParameterizedClass, GenericInstantiateClass) put in the same parent with its template/base class
 * but for write classfile, it's only saved in the host class, the host class is a top class/function.
 *  why select the top class instead of the scope class, i.e.
 *  <pre>
 *      class A{
 *          fun f(){int[] arr}
 *          fun g(){int[] arr}
 *      }
 *  </pre>
 *  select the top class A instead of f or g would share the concrete type int[]
 */
public interface ConcreteType {

    String getFullname();

    List<ClassDef> getConcreteDependencyClasses();

    void acceptRegisterConcreteType(ClassDef hostClass);
}
