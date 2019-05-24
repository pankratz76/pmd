/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */
/* Generated By:JJTree: Do not edit this line. ASTTypeParameter.java */

package net.sourceforge.pmd.lang.java.ast;


import java.util.Optional;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a type parameter declaration of a method, constructor, class or interface declaration.
 *
 * <pre class="grammar">
 *
 * TypeParameter ::= {@linkplain ASTAnnotation Annotation}* &lt;IDENTIFIER&gt; {@link ASTTypeBound TypeBound}?
 *
 * </pre>
 *
 *  @see <a href="https://docs.oracle.com/javase/specs/jls/se9/html/jls-4.html#jls-4.4">JLS</a>
 */
// TODO should implement Annotatable when we use can use Java 8 mixins instead of an abstract class
public final class ASTTypeParameter extends AbstractJavaTypeNode {
    ASTTypeParameter(int id) {
        super(id);
    }


    ASTTypeParameter(JavaParser p, int id) {
        super(p, id);
    }


    /**
     * Returns the name of the type variable introduced by this declaration.
     */
    public String getParameterName() {
        return getImage();
    }


    /**
     * Returns true if this type parameter is bounded,
     * in which case {@link #getTypeBoundNode()} doesn't
     * return {@code null}.
     */
    public boolean hasTypeBound() {
        return getTypeBoundNode() != null;
    }


    /**
     * Returns the type bound node of this parameter,
     * or null if it is not bounded.
     */
    @Nullable
    public ASTType getTypeBoundNode() {
        return Optional.ofNullable(getFirstChildOfType(ASTTypeBound.class))
                       .map(it -> it.getFirstChildOfType(ASTType.class))
                       .orElse(null);
    }


    @Override
    public Object jjtAccept(JavaParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }


    @Override
    public <T> void jjtAccept(SideEffectingVisitor<T> visitor, T data) {
        visitor.visit(this, data);
    }
}
