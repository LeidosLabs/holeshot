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

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Validate the column visibility is a valid expression and set the visibility
 * for a Mutation. See
 * {@link ColumnVisibility#ColumnVisibility(byte[])}
 * for the definition of an expression.
 */
public class ColumnVisibility {

  Node node = null;
  private byte[] expression;

  /**
   * Accessor for the underlying byte string.
   *
   * @return byte array representation of a visibility expression
   */
  public byte[] getExpression() {
    return expression;
  }

  /**
   * The parsedNode types in a parse tree for a visibility expression.
   */
  public static enum NodeType {

    EMPTY, TERM, OR, AND,
  }

  /**
   * All empty nodes are equal and represent the same value.
   */
  private static final Node EMPTY_NODE = new Node(NodeType.EMPTY, 0);

  /**
   * A parsedNode in the parse tree for a visibility expression.
   */
  public static class Node {

    /**
     * An empty list of nodes.
     */
    public static final List<Node> EMPTY = Collections.emptyList();
    NodeType type;
    int start;
    int end;
    List<Node> children = EMPTY;

    public Node(NodeType type, int start) {
      this.type = type;
      this.start = start;
      this.end = start + 1;
    }

    public Node(int start, int end) {
      this.type = NodeType.TERM;
      this.start = start;
      this.end = end;
    }

    public void add(Node child) {
      if (children == EMPTY) {
        children = new ArrayList<>();
      }
      children.add(child);
    }

    public NodeType getType() {
      return type;
    }

    public List<Node> getChildren() {
      return children;
    }

    public int getTermStart() {
      return start;
    }

    public int getTermEnd() {
      return end;
    }

    public ByteSequence getTerm(byte[] expression) {
      if (type != NodeType.TERM) {
        throw new RuntimeException();
      }

      if (expression[start] == '"') {
        // its a quoted term
        int qStart = start + 1;
        int qEnd = end - 1;

        return new ArrayByteSequence(expression, qStart, qEnd - qStart);
      }
      return new ArrayByteSequence(expression, start, end - start);
    }
  }

  /**
   * A parsedNode comparator. Nodes sort according to parsedNode type, terms
   * sort lexicographically. AND and OR nodes sort by number of children, or if
   * the same by corresponding children.
   */
  public static class NodeComparator implements Comparator<Node>, Serializable {

    private static final long serialVersionUID = 1L;
    byte[] text;

    /**
     * Creates a new comparator.
     *
     * @param text expression string, encoded in UTF-8
     */
    public NodeComparator(byte[] text) {
      this.text = text;
    }

    @Override
    public int compare(Node a, Node b) {
      int diff = a.type.ordinal() - b.type.ordinal();
      if (diff != 0) {
        return diff;
      }
      switch (a.type) {
        case EMPTY:
          // All empty nodes are the same
          return 0;
        case TERM:
          return WritableComparator.compareBytes(text, a.start, a.end - a.start, text, b.start, b.end - b.start);
        case OR:
        case AND:
          diff = a.children.size() - b.children.size();
          if (diff != 0) {
            return diff;
          }
          for (int i = 0; i < a.children.size(); i++) {
            diff = compare(a.children.get(i), b.children.get(i));
            if (diff != 0) {
              return diff;
            }
          }
      }
      return 0;
    }
  }

  /*
   * Convience method that delegates to normalize with a new NodeComparator constructed using the supplied expression.
   */
  /**
   * <p>
   * normalize.</p>
   *
   * @param root a
   * {@link ColumnVisibility.Node}
   * object.
   * @param expression an array of byte.
   * @return a
   * {@link ColumnVisibility.Node}
   * object.
   */
  public static Node normalize(Node root, byte[] expression) {
    return normalize(root, expression, new NodeComparator(expression));
  }

  // @formatter:off
  /*
   * Walks an expression's AST in order to:
   *  1) roll up expressions with the same operant (`a&(b&c) becomes a&b&c`)
   *  2) sorts labels lexicographically (permutations of `a&b&c` are re-ordered to appear as `a&b&c`)
   *  3) dedupes labels (`a&b&a` becomes `a&b`)
   */
  // @formatter:on
  /**
   * <p>
   * normalize.</p>
   *
   * @param root a
   * {@link ColumnVisibility.Node}
   * object.
   * @param expression an array of byte.
   * @param comparator a
   * {@link ColumnVisibility.NodeComparator}
   * object.
   * @return a
   * {@link ColumnVisibility.Node}
   * object.
   */
  public static Node normalize(Node root, byte[] expression, NodeComparator comparator) {
    if (root.type != NodeType.TERM) {
      SortedSet<Node> rolledUp = new TreeSet<>(comparator);
      java.util.Iterator<Node> itr = root.children.iterator();
      while (itr.hasNext()) {
        Node c = normalize(itr.next(), expression, comparator);
        if (c.type == root.type) {
          rolledUp.addAll(c.children);
          itr.remove();
        }
      }
      rolledUp.addAll(root.children);
      root.children.clear();
      root.children.addAll(rolledUp);

      // need to promote a child if it's an only child
      if (root.children.size() == 1) {
        return root.children.get(0);
      }
    }

    return root;
  }

  /*
   * Walks an expression's AST and appends a string representation to a supplied StringBuilder. This method adds parens where necessary.
   */
  /**
   * <p>
   * stringify.</p>
   *
   * @param root a
   * {@link ColumnVisibility.Node}
   * object.
   * @param expression an array of byte.
   * @param out a {@link java.lang.StringBuilder} object.
   */
  public static void stringify(Node root, byte[] expression, StringBuilder out) {
    if (root.type == NodeType.TERM) {
      out.append(new String(expression, root.start, root.end - root.start, StandardCharsets.UTF_8));
    } else {
      String sep = "";
      for (Node c : root.children) {
        out.append(sep);
        boolean parens = c.type != NodeType.TERM && root.type != c.type;
        if (parens) {
          out.append("(");
        }
        stringify(c, expression, out);
        if (parens) {
          out.append(")");
        }
        sep = root.type == NodeType.AND ? "&" : "|";
      }
    }
  }

  /**
   * Generates a byte[] that represents a normalized, but logically equivalent,
   * form of this evaluator's expression.
   *
   * @return normalized expression in byte[] form
   */
  public byte[] flatten() {
    Node normRoot = normalize(node, expression);
    StringBuilder builder = new StringBuilder(expression.length);
    stringify(normRoot, expression, builder);
    return builder.toString().getBytes(StandardCharsets.UTF_8);
  }

  private static class ColumnVisibilityParser {

    private int index = 0;
    private int parens = 0;

    public ColumnVisibilityParser() {
    }

    Node parse(byte[] expression) {
      if (expression.length > 0) {
        Node parsedNode = parse_(expression);
        if (parsedNode == null) {
          throw new BadArgumentException("operator or missing parens", new String(expression, StandardCharsets.UTF_8), index - 1);
        }
        if (parens != 0) {
          throw new BadArgumentException("parenthesis mis-match", new String(expression, StandardCharsets.UTF_8), index - 1);
        }
        return parsedNode;
      }
      return null;
    }

    Node processTerm(int start, int end, Node expr, byte[] expression) {
      if (start != end) {
        if (expr != null) {
          throw new BadArgumentException("expression needs | or &", new String(expression, StandardCharsets.UTF_8), start);
        }
        return new Node(start, end);
      }
      if (expr == null) {
        throw new BadArgumentException("empty term", new String(expression, StandardCharsets.UTF_8), start);
      }
      return expr;
    }

    Node parse_(byte[] expression) {
      Node result = null;
      Node expr = null;
      int wholeTermStart = index;
      int subtermStart = index;
      boolean subtermComplete = false;

      while (index < expression.length) {
        switch (expression[index++]) {
          case '&': {
            expr = processTerm(subtermStart, index - 1, expr, expression);
            if (result != null) {
              if (!result.type.equals(NodeType.AND)) {
                throw new BadArgumentException("cannot mix & and |", new String(expression, StandardCharsets.UTF_8), index - 1);
              }
            } else {
              result = new Node(NodeType.AND, wholeTermStart);
            }
            result.add(expr);
            expr = null;
            subtermStart = index;
            subtermComplete = false;
            break;
          }
          case '|': {
            expr = processTerm(subtermStart, index - 1, expr, expression);
            if (result != null) {
              if (!result.type.equals(NodeType.OR)) {
                throw new BadArgumentException("cannot mix | and &", new String(expression, StandardCharsets.UTF_8), index - 1);
              }
            } else {
              result = new Node(NodeType.OR, wholeTermStart);
            }
            result.add(expr);
            expr = null;
            subtermStart = index;
            subtermComplete = false;
            break;
          }
          case '(': {
            parens++;
            if (subtermStart != index - 1 || expr != null) {
              throw new BadArgumentException("expression needs & or |", new String(expression, StandardCharsets.UTF_8), index - 1);
            }
            expr = parse_(expression);
            subtermStart = index;
            subtermComplete = false;
            break;
          }
          case ')': {
            parens--;
            Node child = processTerm(subtermStart, index - 1, expr, expression);
            if (child == null && result == null) {
              throw new BadArgumentException("empty expression not allowed", new String(expression, StandardCharsets.UTF_8), index);
            }
            if (result == null) {
              return child;
            }
            if (result.type == child.type) {
              for (Node c : child.children) {
                result.add(c);
              }
            } else {
              result.add(child);
            }
            result.end = index - 1;
            return result;
          }
          case '"': {
            if (subtermStart != index - 1) {
              throw new BadArgumentException("expression needs & or |", new String(expression, StandardCharsets.UTF_8), index - 1);
            }

            while (index < expression.length && expression[index] != '"') {
              if (expression[index] == '\\') {
                index++;
                if (expression[index] != '\\' && expression[index] != '"') {
                  throw new BadArgumentException("invalid escaping within quotes", new String(expression, StandardCharsets.UTF_8), index - 1);
                }
              }
              index++;
            }

            if (index == expression.length) {
              throw new BadArgumentException("unclosed quote", new String(expression, StandardCharsets.UTF_8), subtermStart);
            }

            if (subtermStart + 1 == index) {
              throw new BadArgumentException("empty term", new String(expression, StandardCharsets.UTF_8), subtermStart);
            }

            index++;

            subtermComplete = true;

            break;
          }
          default: {
            if (subtermComplete) {
              throw new BadArgumentException("expression needs & or |", new String(expression, StandardCharsets.UTF_8), index - 1);
            }

            byte c = expression[index - 1];
            if (!Authorizations.isValidAuthChar(c)) {
              throw new BadArgumentException("bad character (" + c + ")", new String(expression, StandardCharsets.UTF_8), index - 1);
            }
          }
        }
      }
      Node child = processTerm(subtermStart, index, expr, expression);
      if (result != null) {
        result.add(child);
        result.end = index;
      } else {
        result = child;
      }
      if (result.type != NodeType.TERM) {
        if (result.children.size() < 2) {
          throw new BadArgumentException("missing term", new String(expression, StandardCharsets.UTF_8), index);
        }
      }
      return result;
    }
  }

  private void validate(byte[] expression) {
    if (expression != null && expression.length > 0) {
      ColumnVisibilityParser p = new ColumnVisibilityParser();
      node = p.parse(expression);
    } else {
      node = EMPTY_NODE;
    }
    this.expression = expression;
  }

  /**
   * Creates an empty visibility. Normally, elements with empty visibility can
   * be seen by everyone. Though, one could change this behavior with filters.
   *
   * @see #ColumnVisibility(String)
   */
  public ColumnVisibility() {
    this(new byte[]{});
  }

  /**
   * Creates a column visibility for a Mutation.
   *
   * @param expression An expression of the rights needed to see this mutation.
   * The expression is a sequence of characters from the set [A-Za-z0-9_-] along
   * with the binary operators "&amp;" and "|" indicating that both operands are
   * necessary, or that either is necessary. The following are valid expressions
   * for visibility:
   *
   * <pre>
   * A
   * A|B
   * (A|B)&amp;(C|D)
   * orange|(red&amp;yellow)
   *
   * </pre>
   *
   * <P>
   * The following are not valid expressions for visibility:
   *
   * <pre>
   * A|B&amp;C
   * A=B
   * A|B|
   * A&amp;|B
   * ()
   * )
   * dog|!cat
   * </pre>
   *
   * <P>
   * You can use any character you like in your column visibility expression
   * with quoting. If your quoted term contains '&quot;' or '\' then escape them
   * with '\'. The {@link #quote(String)} method will properly quote and escape
   * terms for you.
   *
   * <pre>
   * &quot;A#C&quot;<span></span>&amp;<span></span>B
   * </pre>
   */
  public ColumnVisibility(String expression) {
    this(expression.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Creates a column visibility for a Mutation from a string already encoded in
   * UTF-8 bytes.
   *
   * @param expression visibility expression, encoded as UTF-8 bytes
   * @see #ColumnVisibility(String)
   */
  public ColumnVisibility(byte[] expression) {
    validate(expression);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "[" + new String(expression, StandardCharsets.UTF_8) + "]";
  }

  /**
   * Compares two ColumnVisibilities for string equivalence, not as a meaningful
   * comparison of terms and conditions.
   *
   * @param obj object to compare
   * @return true if this visibility equals the other via string comparison
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ColumnVisibility) {
      ColumnVisibility otherLe = (ColumnVisibility) obj;
      return Arrays.equals(expression, otherLe.expression);
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Arrays.hashCode(expression);
  }

  /**
   * Gets the parse tree for this column visibility.
   *
   * @return parse tree parsedNode
   */
  public Node getParseTree() {
    return node;
  }

  /**
   * Properly quotes terms in a column visibility expression. If no quoting is
   * needed, then nothing is done.
   *
   * <p>
   * Examples of using quote :
   *
   * <pre>
   * import static org.apache.accumulo.core.security.ColumnVisibility.quote;
   *   .
   *   .
   *   .
   * ColumnVisibility cv = new ColumnVisibility(quote(&quot;A#C&quot;) + &quot;&amp;&quot; + quote(&quot;FOO&quot;));
   * </pre>
   *
   * @param term term to quote
   * @return quoted term (unquoted if unnecessary)
   */
  public static String quote(String term) {
    return new String(quote(term.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
  }

  /**
   * Properly quotes terms in a column visibility expression. If no quoting is
   * needed, then nothing is done.
   *
   * @param term term to quote, encoded as UTF-8 bytes
   * @return quoted term (unquoted if unnecessary), encoded as UTF-8 bytes
   * @see #quote(String)
   */
  public static byte[] quote(byte[] term) {
    boolean needsQuote = false;

    for (int i = 0; i < term.length; i++) {
      if (!Authorizations.isValidAuthChar(term[i])) {
        needsQuote = true;
        break;
      }
    }

    if (!needsQuote) {
      return term;
    }

    return VisibilityEvaluator.escape(term, true);
  }
}
