package org.easyweb.groovy.transformation;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.CodeVisitorSupport;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.easyweb.groovy.groovyobject.FileMainClass;
import org.easyweb.util.EasywebLogger;

import java.util.ArrayList;
import java.util.List;

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
public class MethodTransformationInvoker extends CodeVisitorSupport implements ASTTransformation {


    private static List<MethodTransformation> transformations = new ArrayList<MethodTransformation>();

    public static void add(MethodTransformation transformation) {
        transformations.add(transformation);
    }

    public void visit(ASTNode[] nodes, SourceUnit source) {
        EasywebLogger.warn("visit " + source.getName());
        FileMainClass.set(source.getName(), source.getAST().getMainClassName());
        List<MethodNode> methods = source.getAST().getMethods();
        if (!methods.isEmpty()) {
            for (MethodNode method : methods) {
                for (MethodTransformation transformation : transformations) {
                    transformation.transformat(source, method, null);
                }
            }
        }
//        List<ClassNode> classNodes = source.getAST().getClasses();
//        if (!classNodes.isEmpty()) {
//            for (ClassNode classNode : classNodes) {
//                if (classNode instanceof InnerClassNode) {
//                    continue;
//                }
//                for (MethodNode method : classNode.getMethods()) {
//                    GroovyEngine.putScriptMethod(source.getName(), method.getName(), method.getParameters().length);
//                }
//            }
//        }

    }
}
