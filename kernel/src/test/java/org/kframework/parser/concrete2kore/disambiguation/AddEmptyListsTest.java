// Copyright (c) 2015-2016 K Team. All Rights Reserved.
package org.kframework.parser.concrete2kore.disambiguation;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.kframework.attributes.Source;
import org.kframework.definition.Definition;
import org.kframework.definition.Module;
import org.kframework.frontend.K;
import org.kframework.frontend.KApply;
import org.kframework.frontend.KLabel;
import org.kframework.frontend.KORE;
import org.kframework.parser.TreeNodesToKORE;
import org.kframework.parser.concrete2kore.ParseInModule;
import org.kframework.parser.concrete2kore.generator.RuleGrammarGenerator;
import org.kframework.utils.errorsystem.ParseFailedException;
import scala.Tuple2;
import scala.util.Either;

import java.util.Set;

import static org.kframework.frontend.KORE.*;

public class AddEmptyListsTest {
    private ParseInModule parser;

    @Rule
    public TestName testName = new TestName();

    @Before
    public void setUp() throws Exception {
        /*
        FileUtil files = FileUtil.testFileUtil();
        File definitionFile = new File(Kompile.BUILTIN_DIRECTORY.toString() + "/kast.k");
        String baseKText = files.loadFromWorkingDirectory(definitionFile.getPath());
         */
        String baseKText = "require \"domains.k\"\n";
        Definition baseK = org.kframework.DefinitionParser.from(baseKText + DEF, "TEST");
        Module test = baseK.getModule("TEST").get();
        parser = RuleGrammarGenerator.getCombinedGrammar(RuleGrammarGenerator.getRuleGrammar(test, s -> baseK.getModule(s).get()), true);
    }

    private void parseTerm(String term, String sort, K expected) {
        parseTerm(term, sort, expected, 0);
    }

    private void parseTerm(String term, String sort, K expected, int expectWarnings) {
        String source = "AddEmpytListsTest." + testName.getMethodName();
        final Tuple2<Either<Set<ParseFailedException>, K>, Set<ParseFailedException>> parseResult
                = parser.parseString(term, Sort(sort), new Source(source));
        if (parseResult._1().isLeft()) {
            Assert.assertTrue("Unexpected parse errors" + parseResult._1().left().get(), false);
        }
        K actual = TreeNodesToKORE.down(parseResult._1().right().get());
        Assert.assertEquals(expected, actual);
        if (parseResult._2().size() != expectWarnings) {
            Assert.assertTrue("Unexpected parse warnings" + parseResult._2(), false);
        }
    }

    private static final String DEF =
            "module TEST\n" +
                    "syntax A ::= \"a\" [klabel(\"alabel\")]\n" +
                    "syntax B ::= \"b\" [klabel(\"blabel\")]\n" +
                    "syntax A ::= B\n" +
                    "syntax As ::= List{A,\",\"}\n" +
                    "syntax Bs ::= List{B,\",\"}\n" +
                    "syntax As ::= Bs\n" + // TODO: no longer needed. Fixed in #1891 in master branch
                    "syntax Func ::= f(As) | g(A) | h(Bs)" +
                    "endmodule\n";

    public static final KApply NIL = KORE.KApply(KLabel(".List{\"_,_\"}"));
    public static final KLabel CONS = KLabel("_,_");
    public static final KApply A = KORE.KApply(KLabel("alabel"));
    public static final KApply B = KORE.KApply(KLabel("blabel"));
    public static final KLabel F = KLabel("f");
    public static final KLabel G = KLabel("g");
    public static final KLabel H = KLabel("h");
    public static final KLabel CAST_A = KLabel("#SemanticCastToA@TEST");
    public static final KLabel CAST_B = KLabel("#SemanticCastToB@TEST");
    public static final KLabel CAST_AS = KLabel("#SemanticCastToAs@TEST");
    public static final KLabel CAST_BS = KLabel("#SemanticCastToBs@TEST");

    @Test
    public void testEmptyList1() {
        parseTerm(".As", "As", NIL);
    }

    @Ignore("The API of AddEmptyLists needs to change for this to be possible")
    @Test
    public void testItem() {
        parseTerm("a", "As", KORE.KApply(CONS, A, NIL));
    }

    @Test
    public void testConcreteTop() {
        parseTerm(".As", "As", NIL);
        parseTerm("a,a", "As", KORE.KApply(CONS, A, KORE.KApply(CONS, A, NIL)));
        parseTerm("a,.As", "As", KORE.KApply(CONS, A, NIL));
        parseTerm("a,b", "As", KORE.KApply(CONS, A, KORE.KApply(CONS, B, NIL)));
        parseTerm("b,.Bs", "As", KORE.KApply(CONS, B, NIL));
        parseTerm("b,b", "As", KORE.KApply(CONS, B, KORE.KApply(CONS, B, NIL)));
    }

    @Test
    public void testConcreteArgument() {
        parseTerm("f(.As)", "Func", KORE.KApply(F, NIL));
        parseTerm("f(a)", "Func", KORE.KApply(F, KORE.KApply(CONS, A, NIL)));
        parseTerm("f(a,a)", "Func", KORE.KApply(F, KORE.KApply(CONS, A, KORE.KApply(CONS, A, NIL))));
        parseTerm("f(a,.As)", "Func", KORE.KApply(F, KORE.KApply(CONS, A, NIL)));
        parseTerm("f(a,b)", "Func", KORE.KApply(F, KORE.KApply(CONS, A, KORE.KApply(CONS, B, NIL))));
        parseTerm("f(b,.Bs)", "Func", KORE.KApply(F, KORE.KApply(CONS, B, NIL)));
        parseTerm("f(b,b)", "Func", KORE.KApply(F, KORE.KApply(CONS, B, KORE.KApply(CONS, B, NIL))));
    }

    @Ignore("BUG: need to also propagate correct sorts to arguments of labeled application")
    @Test
    public void testLabeledFunSingleItem() {
        parseTerm("`f`(a)", "K", KORE.KApply(F, KORE.KApply(CONS, A, NIL)));
    }

    @Test
    public void testLabedFunConcreteArgument() {
        parseTerm("`f`(.As)", "K", KORE.KApply(F, NIL));
        parseTerm("`f`((a,a))", "K", KORE.KApply(F, KORE.KApply(CONS, A, KORE.KApply(CONS, A, NIL))));
        parseTerm("`f`((a,.As))", "K", KORE.KApply(F, KORE.KApply(CONS, A, NIL)));
        parseTerm("`f`((a,b))", "K", KORE.KApply(F, KORE.KApply(CONS, A, KORE.KApply(CONS, B, NIL))));
        parseTerm("`f`((b,.Bs))", "K", KORE.KApply(F, KORE.KApply(CONS, B, NIL)));
        parseTerm("`f`((b,b))", "K", KORE.KApply(F, KORE.KApply(CONS, B, KORE.KApply(CONS, B, NIL))));
    }

    @Test
    public void testAnnVar() {
        parseTerm("V:As", "K", KORE.KApply(CAST_AS, KVariable("V")));
    }

    @Test
    public void testArgumentLabeledCons() {
        parseTerm("f(`_,_`(a,.As))", "Func", KORE.KApply(F, KORE.KApply(CONS, A, NIL)));
    }

    @Test
    public void testArgumentLabeledNil() {
        parseTerm("f(`.List{\"_,_\"}`(.KList))", "K", KORE.KApply(F, NIL));
    }

    @Test
    public void testArgumentLabeledConsSub1() {
        parseTerm("h(`_,_`(b,.Bs))", "Func", KORE.KApply(H, KORE.KApply(CONS, B, NIL)));
    }

    @Test
    public void testArgumentLabeledConsSub2() {
        // gets a warning because the argument of sort As does not fit.n
        parseTerm("h(`_,_`(a,.As))", "Func", KORE.KApply(H, KORE.KApply(CONS, A, NIL)), 1);
    }

    @Test
    public void testArgumentLabeledNilSub1() {
        parseTerm("h(`.List{\"_,_\"}`(.KList))", "K", KORE.KApply(H, NIL));
    }

    @Test
    public void testArgumentInferredListVar() {
        // 1 warning from inference
        parseTerm("f(V)", "Func", KORE.KApply(F, KORE.KApply(CAST_AS, KVariable("V"))), 1);
    }

    @Test
    public void testArgumentAnnListVar() {
        parseTerm("f(V:As)", "Func", KORE.KApply(F, KORE.KApply(CAST_AS, KVariable("V"))));
    }

    @Test
    public void testArgumentAnnSubListVar() {
        parseTerm("f(V:Bs)", "Func", KORE.KApply(F, KORE.KApply(CAST_BS, KVariable("V"))));
    }

    @Test
    public void testArgumentInferredItemVar() {
        // 1 warning from inference
        parseTerm("f(V)~>g(V)", "Func",
                KSequence(KORE.KApply(F, KORE.KApply(CONS, KORE.KApply(CAST_A, KVariable("V")), NIL)),
                        KORE.KApply(G, KORE.KApply(CAST_A, KVariable("V")))), 1);
    }

    @Test
    public void testArgumentAnnItemVar() {
        parseTerm("f(V:A)", "Func",
                KORE.KApply(F, KORE.KApply(CONS, KORE.KApply(CAST_A, KVariable("V")), NIL)));
    }

    @Test
    public void testArgumentAnnSubItemVar() {
        parseTerm("f(V:B)", "Func",
                KORE.KApply(F, KORE.KApply(CONS, KORE.KApply(CAST_B, KVariable("V")), NIL)));
    }
}
