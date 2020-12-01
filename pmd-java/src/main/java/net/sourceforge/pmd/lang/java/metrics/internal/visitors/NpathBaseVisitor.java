/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.metrics.internal.visitors;

import java.math.BigInteger;

import net.sourceforge.pmd.lang.ast.NodeStream;
import net.sourceforge.pmd.lang.java.ast.ASTConditionalExpression;
import net.sourceforge.pmd.lang.java.ast.ASTDoStatement;
import net.sourceforge.pmd.lang.java.ast.ASTExpression;
import net.sourceforge.pmd.lang.java.ast.ASTForStatement;
import net.sourceforge.pmd.lang.java.ast.ASTIfStatement;
import net.sourceforge.pmd.lang.java.ast.ASTMethodOrConstructorDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTReturnStatement;
import net.sourceforge.pmd.lang.java.ast.ASTStatement;
import net.sourceforge.pmd.lang.java.ast.ASTSwitchArrowBranch;
import net.sourceforge.pmd.lang.java.ast.ASTSwitchBranch;
import net.sourceforge.pmd.lang.java.ast.ASTSwitchExpression;
import net.sourceforge.pmd.lang.java.ast.ASTSwitchFallthroughBranch;
import net.sourceforge.pmd.lang.java.ast.ASTSwitchLabel;
import net.sourceforge.pmd.lang.java.ast.ASTSwitchLike;
import net.sourceforge.pmd.lang.java.ast.ASTSwitchStatement;
import net.sourceforge.pmd.lang.java.ast.ASTTryStatement;
import net.sourceforge.pmd.lang.java.ast.ASTWhileStatement;
import net.sourceforge.pmd.lang.java.ast.JavaNode;
import net.sourceforge.pmd.lang.java.ast.JavaVisitorBase;
import net.sourceforge.pmd.lang.java.metrics.api.JavaMetrics;


/**
 * Visitor for the default n-path complexity version.
 *
 * @author Clément Fournier
 * @author Jason Bennett
 */
public class NpathBaseVisitor extends JavaVisitorBase<Void, BigInteger> {

    /** Instance. */
    public static final NpathBaseVisitor INSTANCE = new NpathBaseVisitor();


    /* Multiplies the complexity of the children of this node. */
    private BigInteger multiplyChildrenComplexities(JavaNode node, Void data) {
        BigInteger product = BigInteger.ONE;

        for (JavaNode child : node.children()) {
            BigInteger childComplexity = child.acceptVisitor(this, data);
            product = product.multiply(childComplexity);
        }

        return product;
    }


    /* Sums the complexity of the children of the node. */
    private BigInteger sumChildrenComplexities(JavaNode node, Void data) {
        BigInteger sum = BigInteger.ZERO;

        for (JavaNode child : node.children()) {
            BigInteger childComplexity = child.acceptVisitor(this, data);
            sum = sum.add(childComplexity);
        }

        return sum;
    }


    @Override
    public BigInteger visitMethodOrCtor(ASTMethodOrConstructorDeclaration node, Void data) {
        return multiplyChildrenComplexities(node, data);
    }


    @Override
    public BigInteger visitJavaNode(JavaNode node, Void data) {
        return multiplyChildrenComplexities(node, data);
    }


    @Override
    public BigInteger visit(ASTIfStatement node, Void data) {
        // (npath of if + npath of else (or 1) + bool_comp of if) * npath of next

        NodeStream<ASTStatement> statementChildren = node.children(ASTStatement.class);

        // add path for not taking if
        BigInteger complexity = node.hasElse() ? BigInteger.ZERO : BigInteger.ONE;

        for (ASTStatement element : statementChildren) {
            complexity = complexity.add(element.acceptVisitor(this, data));
        }

        int boolCompIf = JavaMetrics.booleanExpressionComplexity(node.getFirstChildOfType(ASTExpression.class));
        return complexity.add(BigInteger.valueOf(boolCompIf));
    }


    @Override
    public BigInteger visit(ASTWhileStatement node, Void data) {
        // (npath of while + bool_comp of while + 1) * npath of next

        int boolComp = JavaMetrics.booleanExpressionComplexity(node.getCondition());
        BigInteger nPathBody = node.getBody().acceptVisitor(this, data);
        return nPathBody.add(BigInteger.valueOf(boolComp + 1));
    }


    @Override
    public BigInteger visit(ASTDoStatement node, Void data) {
        // (npath of do + bool_comp of do + 1) * npath of next

        int boolComp = JavaMetrics.booleanExpressionComplexity(node.getCondition());
        BigInteger nPathBody = node.getBody().acceptVisitor(this, data);
        return nPathBody.add(BigInteger.valueOf(boolComp + 1));
    }


    @Override
    public BigInteger visit(ASTForStatement node, Void data) {
        // (npath of for + bool_comp of for + 1) * npath of next

        int boolComp = JavaMetrics.booleanExpressionComplexity(node.getCondition());
        BigInteger nPathBody = node.getBody().acceptVisitor(this, data);
        return nPathBody.add(BigInteger.valueOf(boolComp + 1));
    }


    @Override
    public BigInteger visit(ASTReturnStatement node, Void data) {
        // return statements are valued at 1, or the value of the boolean expression

        ASTExpression expr = node.getExpr();

        if (expr == null) {
            return BigInteger.ONE;
        }

        int boolCompReturn = JavaMetrics.booleanExpressionComplexity(expr);
        BigInteger conditionalExpressionComplexity = multiplyChildrenComplexities(expr, data);

        return conditionalExpressionComplexity.add(BigInteger.valueOf(boolCompReturn));
    }


    @Override
    public BigInteger visit(ASTSwitchExpression node, Void data) {
        return handleSwitch(node, data);
    }

    @Override
    public BigInteger visit(ASTSwitchStatement node, Void data) {
        return handleSwitch(node, data);
    }

    public BigInteger handleSwitch(ASTSwitchLike node, Void data) {
        // bool_comp of switch + sum(npath(case_range))

        int boolCompSwitch = JavaMetrics.booleanExpressionComplexity(node.getFirstChildOfType(ASTExpression.class));

        BigInteger npath = BigInteger.ZERO;
        BigInteger caseRange = BigInteger.ZERO;

        for (ASTSwitchBranch n : node) {

            // Fall-through labels count as 1 for complexity
            if (n instanceof ASTSwitchFallthroughBranch) {
                npath = npath.add(caseRange);
                caseRange = BigInteger.ONE;
            } else if (n instanceof ASTSwitchArrowBranch) {
                npath = npath.add(caseRange);
                caseRange = n.acceptVisitor(this, data);
            } else {
                caseRange = caseRange.multiply(n.acceptVisitor(this, data));
            }
        }
        // add in npath of last label
        return npath.add(caseRange).add(BigInteger.valueOf(boolCompSwitch));
    }

    @Override
    public BigInteger visit(ASTSwitchLabel node, Void data) {
        if (node.isDefault()) {
            return BigInteger.ONE;
        }
        return BigInteger.valueOf(node.children(ASTExpression.class).count());
    }

    @Override
    public BigInteger visit(ASTConditionalExpression node, Void data) {
        // bool comp of guard clause + complexity of last two children (= total - 1)

        int boolCompTernary = JavaMetrics.booleanExpressionComplexity(node.getCondition());

        return sumChildrenComplexities(node, data).add(BigInteger.valueOf(boolCompTernary - 1));
    }


    @Override
    public BigInteger visit(ASTTryStatement node, Void data) {
        /*
         * This scenario was not addressed by the original paper. Based on the
         * principles outlined in the paper, as well as the Checkstyle NPath
         * implementation, this code will add the complexity of the try to the
         * complexities of the catch and finally blocks.
         */
        return sumChildrenComplexities(node, data);
    }
}
