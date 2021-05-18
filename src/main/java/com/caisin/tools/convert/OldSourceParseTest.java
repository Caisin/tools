package com.caisin.tools.convert;

import cn.hutool.core.io.FileUtil;
import com.caisin.tools.utils.FileUtils;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.util.List;

public class OldSourceParseTest {
    public void testTime() {
        int monthValue = LocalDateTime.now().getMonthValue();
        System.out.println("monthValue = " + monthValue);
    }

    public static void main(String[] args) {
        new OldSourceParseTest().astTest();
    }

    public void astTest() {
        String srcDir = "E:/work/Asiainfo/tainjin/code/old/acctmanm-tjin/0506/online/";
//        String srcDir = "E:/work/Asiainfo/tainjin/code/old/acctmanm-tjin/0506/online/acctmanm/src-drecv/com/ailk/acctmanm/tjin/consign/processor";
        String outDir = "E:/code/java/tools/outpath/";

        FileUtils.deepSearchDo(srcDir, (file) -> {
            String fileName = file.getName();
            if (fileName.endsWith(".java") && !fileName.endsWith("Error.java")) {
                try {
                    dealSingleFile(srcDir, outDir, file);
                } catch (Exception e) {
                    System.out.println(file.getAbsolutePath());
                    e.printStackTrace();
                    return false;
                }
            }
            return true;
        });
    }


    private void dealSingleFile(String srcDir, String outDir, File file) throws FileNotFoundException {
        CompilationUnit parse = StaticJavaParser.parse(file);
//        parse.addImport("lombok.extern.slf4j.Slf4j");
        List<MethodCallExpr> nodes = parse.findAll(MethodCallExpr.class);
        dealNodes(parse, nodes);

        List<SimpleName> simpleNames = parse.findAll(SimpleName.class);
        simpleNames.forEach(sm->{
            dealSimpleName(parse,sm);
        });
        List<Name> names = parse.findAll(Name.class);
        names.forEach(this::dealName);

        String outFileName = file.getPath().replaceAll("\\\\", "/").replace(srcDir, outDir);
        File outFile = new File(outFileName);
        if (outFile.exists()) {
            outFile.delete();
        }
        FileUtil.writeUtf8String(parse.toString(), outFileName);
    }

    private void dealNodes(CompilationUnit parse, List<MethodCallExpr> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        for (MethodCallExpr node : nodes) {
            NodeList<Expression> arguments = node.getArguments();
            String identifier = node.getName().getIdentifier();
            final Expression first = (arguments != null && arguments.isNonEmpty()) ? arguments.getFirst().get() : null;
            final String firstStr = first != null ? first.toString() : "";
            if ("getSysDate".equals(identifier)) {
                if (firstStr.startsWith("\"")) {
                    node.setName(new SimpleName("getSysTime"));
                }
            }
            if (identifier.startsWith("getSeqId")) {
                node.getScope().ifPresent(
                        scope -> {
                            if (!arguments.isEmpty()) {
                                if ("SeqHelper".equals(scope.toString())) {
                                    arguments.remove(0);
                                    arguments.addFirst(new NameExpr("Seqs." + firstStr.replaceAll("\"", "")));
                                    parse.addImport("com.asiainfo.ams.core.pub.sequence.Seqs");
                                    parse.addImport("com.asiainfo.ams.acctcomp.database.oracle.sequence.SeqHelper");

                                    addParentNode(node, "String", "valueOf");
                                }
                            }
                        }
                );
            }
            if ("getCommPara".equals(identifier)) {
                node.getScope().ifPresent(
                        scope -> {
                            addParentNode(node, "CopyUtil", "copyToMap");
                            parse.addImport("com.asiainfo.ams.acctcomp.util.CopyUtil");
                        }
                );
            }

            if ("getStaticValue".equals(identifier)) {
                node.getScope().ifPresent(
                        scope -> {

                            if (!arguments.isEmpty()) {
                                if ("ParamMgr".equals(scope.toString())) {
                                    arguments.remove(0);
                                    arguments.addFirst(new NameExpr("ParamTables." + firstStr.replaceAll("\"", "")));
                                    parse.addImport("com.asiainfo.ams.core.pub.enums.ParamTables");
                                }
                            }
                        });
            }
            if ("getString".equals(identifier)
                    || "getInt".equals(identifier)
                    || "getLong".equals(identifier)
                    || "getBoolean".equals(identifier)
            ) {
                node.getScope().ifPresent(
                        scope -> {
                            if (!arguments.isEmpty()) {
                                arguments.addFirst(scope);
                                node.setScope(new NameExpr("Maps"));
                                node.setArguments(arguments);
                                parse.addImport("com.asiainfo.ams.acctcomp.util.data.Maps");
                                if (firstStr.equals("\"ACCT_ID\"")) {
                                    node.setName("getLong");
                                }
                            }
                        }
                );
            }
            if ("getData".equals(identifier)) {
                node.getScope().ifPresent(
                        scope -> {
                            if (!arguments.isEmpty()) {
                                if ("ParamMgr".equals(scope.toString())) {
                                    arguments.remove(0);
                                    arguments.addFirst(new NameExpr("ParamTables." + firstStr.replaceAll("\"", "")));
                                    parse.addImport("com.asiainfo.ams.core.pub.enums.ParamTables");
                                } else {
                                    if (firstStr.startsWith("\"")) {
                                        arguments.addFirst(scope);
                                        node.setArguments(arguments);
                                        node.setScope(new NameExpr("Maps"));
                                        node.setName(new SimpleName("getMap"));
                                        parse.addImport("com.asiainfo.ams.acctcomp.util.data.Maps");
                                    } else {
                                        node.setName(new SimpleName("get"));
                                    }
                                }
                            }
                        }
                );
            }
            if ("getContext".equals(identifier)) {
                if (!node.getScope().isPresent()) {
                    node.setName("getVisit");
                }
            }

            if ("getDataset".equals(identifier)) {
                node.getScope().ifPresent(
                        scope -> {
                            if (!arguments.isEmpty()) {
                                arguments.addFirst(scope);
                                node.setScope(new NameExpr("Maps"));
                                node.setArguments(arguments);
                                node.setName(new SimpleName("getList"));
                                parse.addImport("com.asiainfo.ams.acctcomp.util.data.Maps");
                            }
                        }
                );
            }
            if ("put".equals(identifier)) {
                node.getScope().ifPresent(scope -> {
                    if (scope instanceof MethodCallExpr) {
                        MethodCallExpr call = (MethodCallExpr) scope;
                        SimpleName name = call.getName();
                        if ("first".equals(name.getIdentifier())) {
                            name.setIdentifier("get");
                            arguments.addFirst(new NameExpr("0"));
                            call.setArguments(arguments);
                        }
                    }

                });
            }
        }
    }

    private void addParentNode(MethodCallExpr node, String pScope, String pName) {
        MethodCallExpr clone = new MethodCallExpr();
        clone.setArguments(node.getArguments());
        clone.setName(node.getName().getIdentifier());
        clone.setScope(new NameExpr(node.getScope().get().toString()));
        NodeList<Expression> list = new NodeList<>();
        list.add(clone);
        node.setArguments(list);
        node.setScope(new NameExpr(pScope));
        node.setName(pName);
    }


    private void dealName(Name name) {
        String identifier = name.getIdentifier();
        if ("IData".equals(identifier)) {
            name.setIdentifier("Map");
            name.setQualifier(new Name("java.util"));
        }
        if ("IDataset".equals(identifier)) {
            name.setIdentifier("List");
            name.setQualifier(new Name("java.util"));
        }
        if ("DataMap".equals(identifier)) {
            name.setIdentifier("HashMap");
            name.setQualifier(new Name("java.util"));
        }
        if ("DatasetList".equals(identifier)) {
            name.setIdentifier("ArrayList");
            name.setQualifier(new Name("java.util"));
        }
        if ("Cycle".equals(identifier)) {
            name.setIdentifier("CycleDTO");
            name.setQualifier(new Name("com.asiainfo.ams.acctcomp.api.business.param.dto"));
        }

        if (!(identifier.equals("Exception") || identifier.equals("throwException")) && identifier.endsWith("Exception")) {
            String error = identifier.replace("Exception", "Error");
            name.setIdentifier(error);
            name.setQualifier(new Name("com.asiainfo.ams.core.pub.exception"));
        }

        //
        if ("TimeUtil".equals(identifier)) {
            name.setQualifier(new Name("com.asiainfo.ams.acctcomp.util"));
        }


    }

    private void dealSimpleName(CompilationUnit parse, SimpleName simpleName) {
        String identifier = simpleName.getIdentifier();
        if ("IData".equals(identifier)) {
            simpleName.setIdentifier("Map<String, Object>");
        }
        if ("IDataset".equals(identifier)) {
            simpleName.setIdentifier("List<Map<String, Object>>");
        }
        if ("DataMap".equals(identifier)) {
            simpleName.setIdentifier("HashMap<>");
        }
        if ("DatasetList".equals(identifier)) {
            simpleName.setIdentifier("ArrayList<>");
        }
        if ("Cycle".equals(identifier)) {
            simpleName.setIdentifier("CycleDTO");
        }

        if ("TaskConfig".equals(identifier)) {
//            simpleName.setIdentifier("CycleDTO");
            simpleName.setIdentifier("TaskContext.getTaskDealParam()");
            parse.addImport("com.asiainfo.ams.acctcomp.session.TaskContext");
            Node node = simpleName.getParentNode().get().getParentNode().get();
            if(node instanceof FieldAccessExpr){
                SimpleName name = ((FieldAccessExpr) node).getName();
                String nameIdentifier = name.getIdentifier();
                String s = "get" + (nameIdentifier.charAt(0) + "").toUpperCase() + nameIdentifier.substring(1) + "()";
                if ("databaseNo".equals(nameIdentifier)) {
                    s="getCenter()";
                }
                name.setIdentifier(s);
            }
            System.out.println("node = " + node);
        }

       /* simpleName.getParentNode().ifPresent(n->{
            String pStr = n.toString();
            if (!"TaskConfig".equals(identifier)
//                   &&pStr.contains("TaskConfig")
                    &&pStr.endsWith("TaskConfig."+identifier)
            ) {
                String s = "get" + (identifier.charAt(0) + "").toUpperCase() + identifier.substring(1) + "()";
                if ("databaseNo".equals(identifier)) {
                    s="getCenter()";
                }
                simpleName.setIdentifier(s);
                System.out.println("identifier = " + identifier);
            }
        });*/

        if (!(identifier.equals("Exception") || identifier.equals("throwException")) && identifier.endsWith("Exception")) {
            String error = identifier.replace("Exception", "Error");
            simpleName.setIdentifier(error);
        }
    }
}
