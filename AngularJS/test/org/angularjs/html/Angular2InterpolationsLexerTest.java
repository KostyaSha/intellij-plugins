package org.angularjs.html;

import com.intellij.lang.javascript.JSElementTypes;
import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.Lexer;
import com.intellij.lexer.MergingLexerAdapter;
import com.intellij.openapi.application.PathManager;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.testFramework.LexerTestCase;
import org.angularjs.AngularTestUtil;

/**
 * @author Dennis.Ushakov
 */
public class Angular2InterpolationsLexerTest extends LexerTestCase {
  public void testInterpolation() {
    doTest("{{interpolated}}", "XML_DATA_CHARACTERS ('{{')\n" +
                               "JS:EMBEDDED_EXPRESSION ('interpolated')\n" +
                               "XML_DATA_CHARACTERS ('}}')");

    doTest("{{interpolated}}{{again}}", "XML_DATA_CHARACTERS ('{{')\n" +
                                        "JS:EMBEDDED_EXPRESSION ('interpolated')\n" +
                                        "XML_DATA_CHARACTERS ('}}{{')\n" +
                                        "JS:EMBEDDED_EXPRESSION ('again')\n" +
                                        "XML_DATA_CHARACTERS ('}}')");

    doTest("{{interpolated}}with{{text}}", "XML_DATA_CHARACTERS ('{{')\n" +
                                           "JS:EMBEDDED_EXPRESSION ('interpolated')\n" +
                                           "XML_DATA_CHARACTERS ('}}with{{')\n" +
                                           "JS:EMBEDDED_EXPRESSION ('text')\n" +
                                           "XML_DATA_CHARACTERS ('}}')");

    doTest("more{{interpolated}}with{{text}}again", "XML_DATA_CHARACTERS ('more{{')\n" +
                                                    "JS:EMBEDDED_EXPRESSION ('interpolated')\n" +
                                                    "XML_DATA_CHARACTERS ('}}with{{')\n" +
                                                    "JS:EMBEDDED_EXPRESSION ('text')\n" +
                                                    "XML_DATA_CHARACTERS ('}}again')");
  }

  @Override
  protected Lexer createLexer() {
    final _AngularJSInterpolationsLexer lexer = new _AngularJSInterpolationsLexer(null);
    lexer.setType(XmlTokenType.XML_DATA_CHARACTERS);
    return new MergingLexerAdapter(new FlexAdapter(lexer), TokenSet.create(JSElementTypes.EMBEDDED_CONTENT, XmlTokenType.XML_DATA_CHARACTERS));
  }

  @Override
  protected String getDirPath() {
    return AngularTestUtil.getBaseTestDataPath(Angular2InterpolationsLexerTest.class).substring(PathManager.getHomePath().length());
  }
}
