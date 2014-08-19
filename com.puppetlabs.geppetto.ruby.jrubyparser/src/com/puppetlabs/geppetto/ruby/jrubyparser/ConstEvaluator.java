package com.puppetlabs.geppetto.ruby.jrubyparser;

import java.util.List;
import java.util.Map;

import org.jrubyparser.ast.ArrayNode;
import org.jrubyparser.ast.CallNode;
import org.jrubyparser.ast.Colon2Node;
import org.jrubyparser.ast.ConstNode;
import org.jrubyparser.ast.HashNode;
import org.jrubyparser.ast.ListNode;
import org.jrubyparser.ast.Node;
import org.jrubyparser.ast.StrNode;
import org.jrubyparser.ast.SymbolNode;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Evaluates (a limited set of) Ruby constant expressions. TODO: Colon3Node
 * (i.e. name relative to global root) not handled - as FQN are returned as list
 * of String there is currently no marker if it is relative or absolute.
 *
 */
public class ConstEvaluator extends AbstractJRubyVisitor {
	private List<String> addAll(List<String> a, List<String> b) {
		a.addAll(b);
		return a;
	}

	public Object eval(Node node) {
		if(node == null)
			return null;
		// Can't visit ListNode, because they are not supposed to be "evaluated" (duh! impl sucks).
		// Must compare against exact class since everything derived implements "accept".
		if(node.getClass() == ListNode.class)
			return this.visitListNode((ListNode) node);
		return node.accept(this);
	}

	private List<String> splice(Object a, Object b) {
		return addAll(stringList(a), stringList(b));
	}

	@SuppressWarnings("unchecked")
	public List<String> stringList(Object x) {
		if(x instanceof List)
			return (List<String>) x; // have faith
		if(x instanceof String)
			return Lists.newArrayList((String) x);
		if(x == null)
			return Lists.newArrayList(); // empty list
		throw new IllegalArgumentException("Not a string or lists of strings");
	}

	@Override
	public Object visitArrayNode(ArrayNode iVisited) {
		List<Object> result = Lists.newArrayList();
		for(Node n : iVisited.childNodes())
			result.add(eval(n));
		return result;
	}

	@Override
	public Object visitCallNode(CallNode iVisited) {
		if("intern".equals(iVisited.getName()))
			return eval(iVisited.getReceiver());
		return null;
	}

	@Override
	public Object visitColon2Node(Colon2Node iVisited) {
		return splice(eval(iVisited.getLeftNode()), iVisited.getName());
	}

	@Override
	public Object visitConstNode(ConstNode iVisited) {
		return iVisited.getName();
	}

	@Override
	public Object visitHashNode(HashNode iVisited) {
		Map<Object, Object> result = Maps.newHashMap();
		List<Node> children = iVisited.childNodes();
		children = children.get(0).childNodes();
		for(int i = 0; i < children.size(); i++) {
			Object key = eval(children.get(i++));
			Object value = eval(children.get(i));
			result.put(key, value);
		}
		return result;
	}

	public Object visitListNode(ListNode iVisited) {
		List<Object> result = Lists.newArrayList();
		for(Node n : iVisited.childNodes())
			result.add(eval(n));
		return result;
	}

	@Override
	public Object visitStrNode(StrNode iVisited) {
		return iVisited.getValue();
	}

	@Override
	public Object visitSymbolNode(SymbolNode iVisited) {
		return iVisited.getName();
	}
}
