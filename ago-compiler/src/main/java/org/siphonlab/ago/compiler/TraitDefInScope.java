package org.siphonlab.ago.compiler;

import org.siphonlab.ago.compiler.parser.AgoParser;

import java.util.Collection;
import java.util.Map;

/**
 * use trait as scope member, i.e. trait.this, trait.this.xxx, super.xxx in descendant of trait
 * it's different with using outside, i.e. var t a SomeTrait = obj, t.traitMethod
 */
public class TraitDefInScope extends TraitDef{
    private final ClassDef baseTrait;

    public TraitDefInScope(ClassDef baseTrait) {
        super(baseTrait.name, getDeclaration(baseTrait));
        this.baseTrait = baseTrait;
        this.setGenericSource(baseTrait.getGenericSource());
        this.setModifiers(baseTrait.modifiers);
        this.setUnit(baseTrait.getUnit());
        this.parent = baseTrait.parent;
    }

    private static AgoParser.TraitDeclarationContext getDeclaration(ClassDef baseTrait) {
        if(baseTrait instanceof TraitDef b){
            return b.getTraitDeclaration();
        } else if(baseTrait.getGenericSource() != null) {
            var g = baseTrait.getGenericSource();
            return getDeclaration(g.originalTemplate());
        } else if(baseTrait instanceof ParameterizedClassDef parameterizedClassDef){
            return getDeclaration(parameterizedClassDef.getBaseClass());
        }
        throw new RuntimeException("'%s' cannot wrap to trait".formatted(baseTrait));
    }

    private static AgoParser.DeclarationTypeContext getPermitDeclaration(ClassDef baseTrait) {
        if(baseTrait instanceof TraitDef b){
            return b.getPermitTypeDecl();
        } else if(baseTrait.getGenericSource() != null) {
            var g = baseTrait.getGenericSource();
            assert g.originalTemplate() instanceof TraitDef;
            return ((TraitDef) g.originalTemplate()).getPermitTypeDecl();
        } else if (baseTrait instanceof ParameterizedClassDef parameterizedClassDef) {
            assert parameterizedClassDef.getBaseClass() instanceof TraitDef;
            return ((TraitDef) parameterizedClassDef.getBaseClass()).getPermitTypeDecl();
        } else {
            throw new RuntimeException("'%s' cannot wrap to trait".formatted(baseTrait));
        }
    }

    @Override
    public Collection<ClassDef> getChildren(String name) {
        return baseTrait.getChildren(name);
    }

    @Override
    public NamespaceCollection<ClassDef> getChildren() {
        return baseTrait.getChildren();
    }

    @Override
    public ConstructorDef getConstructor() {
        return baseTrait.getConstructor();
    }

    @Override
    public ClassDef getChild(String name) {
        return baseTrait.getChild(name);
    }

    @Override
    public Map<String, Field> getFields() {
        return baseTrait.getFields();
    }

    @Override
    public ClassDef getPermitClass() {
        return baseTrait.getPermitClass();
    }

    @Override
    public AgoParser.DeclarationTypeContext getPermitTypeDecl() {
        return getPermitDeclaration(baseTrait);
    }

    @Override
    public String getFullname() {
        return baseTrait.getFullname();
    }

    @Override
    public CompilingStage getCompilingStage() {
        return baseTrait.getCompilingStage();
    }

    @Override
    public String toString() {
        return baseTrait.toString();
    }

    @Override
    public Field getFieldForPermitClass() {
        return baseTrait.getFieldForPermitClass();
    }
}
