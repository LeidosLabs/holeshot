/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.leidoslabs.holeshot.security.reuse;

import static com.leidoslabs.holeshot.security.reuse.ColumnVisibility.quote;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;

import org.junit.Test;

public class ColumnVisibilityTest {

  private void shouldThrow(String... strings) {
    for (String s : strings) {
      try {
        new ColumnVisibility(s.getBytes());
        fail("Should throw: " + s);
      } catch (IllegalArgumentException e) {
        // expected
      }
    }
  }

  private void shouldNotThrow(String... strings) {
    for (String s : strings) {
      new ColumnVisibility(s.getBytes());
    }
  }

  @Test
  public void testEmpty() {
    // empty visibility is valid
    ColumnVisibility a = new ColumnVisibility();
    ColumnVisibility b = new ColumnVisibility(new byte[0]);
    ColumnVisibility c = new ColumnVisibility("");

    assertEquals(a, b);
    assertEquals(a, c);
  }

  @Test
  public void testEmptyFlatten() {
    // empty visibility is valid
    new ColumnVisibility().flatten();
    new ColumnVisibility("").flatten();
  }

  @Test
  public void testSimple() {
    shouldNotThrow("test", "(one)");
  }

  @Test
  public void testCompound() {
    shouldNotThrow("a|b", "a&b", "ab&bc");
    shouldNotThrow("A&B&C&D&E", "A|B|C|D|E", "(A|B|C)", "(A)|B|(C)", "A&(B)&(C)", "A&B&(L)");
    shouldNotThrow("_&-&:");
  }

  @Test
  public void testBadCharacters() {
    shouldThrow("=", "*", "^", "%", "@");
    shouldThrow("a*b");
  }

  public void normalized(String... values) {
    for (int i = 0; i < values.length; i += 2) {
      ColumnVisibility cv = new ColumnVisibility(values[i].getBytes());
      assertArrayEquals(cv.flatten(), values[i + 1].getBytes());
    }
  }

  @Test
  public void testComplexCompound() {
    shouldNotThrow("(a|b)&(x|y)");
    shouldNotThrow("a&(x|y)", "(a|b)&(x|y)", "A&(L|M)", "B&(L|M)", "A&B&(L|M)");
    shouldNotThrow("A&FOO&(L|M)", "(A|B)&FOO&(L|M)", "A&B&(L|M|FOO)", "((A|B|C)|foo)&bar");
    shouldNotThrow("(one&two)|(foo&bar)", "(one|foo)&three", "one|foo|bar", "(one|foo)|bar", "((one|foo)|bar)&two");
  }

  @Test
  public void testNormalization() {
    normalized("a", "a", "(a)", "a", "b|a", "a|b", "(b)|a", "a|b", "(b|(a|c))&x", "x&(a|b|c)", "(((a)))", "a");
    final String normForm = "a&b&c";
    normalized("b&c&a", normForm, "c&b&a", normForm, "a&(b&c)", normForm, "(a&c)&b", normForm);

    // this an expression that's basically `expr | expr`
    normalized("(d&c&b&a)|(b&c&a&d)", "a&b&c&d");
  }

  @Test
  public void testDanglingOperators() {
    shouldThrow("a|b&");
    shouldThrow("(|a)");
    shouldThrow("|");
    shouldThrow("a|", "|a", "|", "&");
    shouldThrow("&(five)", "|(five)", "(five)&", "five|", "a|(b)&", "(&five)", "(five|)");
  }

  @Test
  public void testMissingSeparators() {
    shouldThrow("one(five)", "(five)one", "(one)(two)", "a|(b(c))");
  }

  @Test
  public void testMismatchedParentheses() {
    shouldThrow("(", ")", "(a&b", "b|a)", "A|B)");
  }

  @Test
  public void testMixedOperators() {
    shouldThrow("(A&B)|(C&D)&(E)");
    shouldThrow("a|b&c", "A&B&C|D", "(A&B)|(C&D)&(E)");
  }

  @Test
  public void testQuotes() {
    shouldThrow("\"\"");
    shouldThrow("\"A\"A");
    shouldThrow("\"A\"\"B\"");
    shouldThrow("(A)\"B\"");
    shouldThrow("\"A\"(B)");
    shouldThrow("\"A");
    shouldThrow("\"");
    shouldThrow("\"B");
    shouldThrow("A&\"B");
    shouldThrow("A&\"B\\'");

    shouldNotThrow("\"A\"");
    shouldNotThrow("(\"A\")");
    shouldNotThrow("A&\"B.D\"");
    shouldNotThrow("A&\"B\\\\D\"");
    shouldNotThrow("A&\"B\\\"D\"");
  }

  @Test
  public void testToString() {
    ColumnVisibility cv = new ColumnVisibility(ColumnVisibility.quote("a"));
    assertEquals("[a]", cv.toString());

    // multi-byte
    cv = new ColumnVisibility(ColumnVisibility.quote("五"));
    assertEquals("[\"五\"]", cv.toString());
  }

  @Test
  public void testParseTree() {
    ColumnVisibility.Node node = parse("(W)|(U&V)");
    assertNode(node, ColumnVisibility.NodeType.OR, 0, 9);
    assertNode(node.getChildren().get(0), ColumnVisibility.NodeType.TERM, 1, 2);
    assertNode(node.getChildren().get(1), ColumnVisibility.NodeType.AND, 5, 8);
  }

  @Test
  public void testParseTreeWithNoChildren() {
    ColumnVisibility.Node node = parse("ABC");
    assertNode(node, ColumnVisibility.NodeType.TERM, 0, 3);
  }

  @Test
  public void testParseTreeWithTwoChildren() {
    ColumnVisibility.Node node = parse("ABC|DEF");
    assertNode(node, ColumnVisibility.NodeType.OR, 0, 7);
    assertNode(node.getChildren().get(0), ColumnVisibility.NodeType.TERM, 0, 3);
    assertNode(node.getChildren().get(1), ColumnVisibility.NodeType.TERM, 4, 7);
  }

  @Test
  public void testParseTreeWithParenthesesAndTwoChildren() {
    ColumnVisibility.Node node = parse("(ABC|DEF)");
    assertNode(node, ColumnVisibility.NodeType.OR, 1, 8);
    assertNode(node.getChildren().get(0), ColumnVisibility.NodeType.TERM, 1, 4);
    assertNode(node.getChildren().get(1), ColumnVisibility.NodeType.TERM, 5, 8);
  }

  @Test
  public void testParseTreeWithParenthesizedChildren() {
    ColumnVisibility.Node node = parse("ABC|(DEF&GHI)");
    assertNode(node, ColumnVisibility.NodeType.OR, 0, 13);
    assertNode(node.getChildren().get(0), ColumnVisibility.NodeType.TERM, 0, 3);
    assertNode(node.getChildren().get(1), ColumnVisibility.NodeType.AND, 5, 12);
    assertNode(node.getChildren().get(1).children.get(0), ColumnVisibility.NodeType.TERM, 5, 8);
    assertNode(node.getChildren().get(1).children.get(1), ColumnVisibility.NodeType.TERM, 9, 12);
  }

  @Test
  public void testParseTreeWithMoreParentheses() {
    ColumnVisibility.Node node = parse("(W)|(U&V)");
    assertNode(node, ColumnVisibility.NodeType.OR, 0, 9);
    assertNode(node.getChildren().get(0), ColumnVisibility.NodeType.TERM, 1, 2);
    assertNode(node.getChildren().get(1), ColumnVisibility.NodeType.AND, 5, 8);
    assertNode(node.getChildren().get(1).children.get(0), ColumnVisibility.NodeType.TERM, 5, 6);
    assertNode(node.getChildren().get(1).children.get(1), ColumnVisibility.NodeType.TERM, 7, 8);
  }

  @Test
  public void testEmptyParseTreesAreEqual() {
    Comparator<ColumnVisibility.Node> comparator = new ColumnVisibility.NodeComparator(new byte[]{});
    ColumnVisibility.Node empty = new ColumnVisibility().getParseTree();
    assertEquals(0, comparator.compare(empty, parse("")));
  }

  @Test
  public void testParseTreesOrdering() {
    byte[] expression = "(b&c&d)|((a|m)&y&z)|(e&f)".getBytes(StandardCharsets.UTF_8);
    byte[] flattened = new ColumnVisibility(expression).flatten();

    // Convert to String for indexOf convenience
    String flat = new String(flattened, StandardCharsets.UTF_8);
    assertTrue("shortest expressions sort first", flat.indexOf('e') < flat.indexOf('|'));
    assertTrue("shortest children sort first", flat.indexOf('b') < flat.indexOf('a'));
  }

  private ColumnVisibility.Node parse(String s) {
    ColumnVisibility v = new ColumnVisibility(s);
    return v.getParseTree();
  }

  private void assertNode(ColumnVisibility.Node node, ColumnVisibility.NodeType nodeType, int start, int end) {
    assertEquals(node.type, nodeType);
    assertEquals(start, node.start);
    assertEquals(end, node.end);
  }
}
