/**
 * Copyright (c) 2013 Puppet Labs, Inc. and other contributors, as listed below.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Puppet Labs
 */
package com.puppetlabs.geppetto.ruby.jrubyparser;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jrubyparser.ast.BlockAcceptingNode;
import org.jrubyparser.ast.BlockNode;
import org.jrubyparser.ast.BlockPassNode;
import org.jrubyparser.ast.CallNode;
import org.jrubyparser.ast.ClassNode;
import org.jrubyparser.ast.FCallNode;
import org.jrubyparser.ast.IArgumentNode;
import org.jrubyparser.ast.InstAsgnNode;
import org.jrubyparser.ast.IterNode;
import org.jrubyparser.ast.NewlineNode;
import org.jrubyparser.ast.Node;
import org.jrubyparser.ast.NodeType;
import org.jrubyparser.ast.RootNode;
import org.jrubyparser.ast.VCallNode;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.puppetlabs.geppetto.ruby.PPProviderInfo;
import com.puppetlabs.geppetto.ruby.PPTypeInfo;

/**
 * Find puppet resource type definition(s) in a ruby file.
 */
public class PPTypeFinder {
	private static class OpCallVisitor extends AbstractJRubyVisitor {
		/**
		 * Returned when a visited node detect it is not meaningful to visit its
		 * children.
		 */
		public static final Object DO_NOT_VISIT_CHILDREN = new Object();

		private final ConstEvaluator constEvaluator;

		private String name = null;

		private String[] fqn = null;

		OpCallVisitor(ConstEvaluator constEvaluator) {
			this.constEvaluator = constEvaluator;
		}

		/**
		 * Visits all nodes in graph, and if visitor returns non-null, the
		 * iteration stops and the returned non-null value is returned.
		 *
		 * @param root
		 * @return
		 */
		private Object findOpCall(Node root) {
			Object r = null;
			if(root.getNodeType() == NodeType.CALLNODE)
				r = root.accept(this);
			if(r != DO_NOT_VISIT_CHILDREN) {
				if(r != null) {
					return r;
				}
				for(Node n : root.childNodes()) {
					r = findOpCall(n);
					if(r != null)
						return r;
				}
			}
			return null;
		}

		public CallNode findOpCall(Node root, String name, String... receiverFQN) {
			this.name = name;
			this.fqn = receiverFQN;
			return (CallNode) findOpCall(root);
		}

		/**
		 * Finds fqn[0..n].name
		 */
		@Override
		public Object visitCallNode(CallNode iVisited) {
			if(!name.equals(iVisited.getName()))
				return null;
			Object x = constEvaluator.eval(iVisited.getReceiver());
			if(!(x instanceof List<?>))
				return null;
			List<?> receiver = (List<?>) x;
			if(receiver.size() != fqn.length)
				return null;
			for(int i = 0; i < fqn.length; i++)
				if(!fqn[i].equals(receiver.get(i)))
					return null;

			return iVisited;
		}
	}

	private final static String NEWPARAM = "newparam";

	private final static String NEWPROPERTY = "newproperty";

	private final static String NEWTYPE = "newtype";

	private final static String DESC = "desc";

	private final static String ENSURABLE = "ensurable";

	private final static String NEWCHECK = "newcheck";

	private final static String[] PUPPET_PROVIDE_FQN = new String[] { "Puppet", "Type", "type", "provide" };

	private final static String[] PUPPET_NEWTYPE_FQN = new String[] { "Puppet", "Type", "newtype" };

	private final static String[] PUPPET_NAGIOS_NEWTYPE_FQN = new String[] { "Nagios", "Base", "newtype" };

	private PPTypeInfo createNagiosTypeInfo(RubyFinder finder, Node root, String typeName) {

		if(root == null)
			return null;
		Map<String, PPTypeInfo.Entry> propertyMap = Maps.newHashMap();
		Map<String, PPTypeInfo.Entry> parameterMap = Maps.newHashMap();
		String typeDocumentation = "";

		// All nagios types have these nagios meta types
		// (added in nagios_maker.rb - seems lots of work to parse that file to
		// only find
		// these, so hardcoding them here).
		//
		parameterMap.put("ensure", new PPTypeInfo.Entry("", false, false));
		parameterMap.put("target", new PPTypeInfo.Entry("", false, false));

		for(Node n : root.childNodes()) {
			if(n.getNodeType() == NodeType.NEWLINENODE)
				n = ((NewlineNode) n).getNextNode();

			switch(n.getNodeType()) {
				case FCALLNODE:
					FCallNode callNode = (FCallNode) n;
					if("setparameters".equals(callNode.getName()))
						for(String name : getArgs(finder, callNode))
							parameterMap.put(name, getEntry(finder, callNode));
					break;
				case VCALLNODE:
					VCallNode vcallNode = (VCallNode) n;
					// A call to 'ensurable' adds 'ensure' parameter
					if(ENSURABLE.equals(vcallNode.getName()))
						parameterMap.put("ensure", new PPTypeInfo.Entry("", false, false));
					break;
				case INSTASGNNODE:
					InstAsgnNode docNode = (InstAsgnNode) n;
					if("doc".equals(docNode.getName())) {
						typeDocumentation = getStringArgDefault(finder, docNode.getValue(), "");
					}
					break;
				default:
					break;
			}
		}
		PPTypeInfo typeInfo = new PPTypeInfo(typeName, typeDocumentation, propertyMap, parameterMap);
		return typeInfo;
	}

	private PPProviderInfo createProviderInfo(RubyFinder finder, Node root, String typeName, String providerName) {
		if(root == null)
			return null;
		String documentation = "";

		for(Node n : root.childNodes()) {
			if(n.getNodeType() == NodeType.NEWLINENODE)
				n = ((NewlineNode) n).getNextNode();

			if(n instanceof FCallNode) {
				FCallNode callNode = (FCallNode) n;
				if(DESC.equals(callNode.getName())) {
					documentation = getFirstArg(finder, callNode);
					break;
				}
			}
		}
		return new PPProviderInfo(typeName, providerName, documentation);
	}

	private PPTypeInfo createTypeInfo(RubyFinder finder, Node root, String typeName) {
		if(root == null)
			return null;
		Map<String, PPTypeInfo.Entry> propertyMap = Maps.newHashMap();
		Map<String, PPTypeInfo.Entry> parameterMap = Maps.newHashMap();
		String typeDocumentation = null;

		for(Node n : root.childNodes()) {
			if(n.getNodeType() == NodeType.NEWLINENODE)
				n = ((NewlineNode) n).getNextNode();

			switch(n.getNodeType()) {
				case FCALLNODE:
					FCallNode callNode = (FCallNode) n;
					if(NEWPARAM.equals(callNode.getName()))
						parameterMap.put(getFirstArg(finder, callNode), getEntry(finder, callNode));
					else if(NEWCHECK.equals(callNode.getName()))
						parameterMap.put(getFirstArg(finder, callNode), getEntry(finder, callNode));
					else if(NEWPROPERTY.equals(callNode.getName()))
						propertyMap.put(getFirstArg(finder, callNode), getEntry(finder, callNode));
					else if(ENSURABLE.equals(callNode.getName()))
						parameterMap.put("ensure", getEntry(finder, callNode));
					else if(typeDocumentation == null && DESC.equals(callNode.getName()))
						typeDocumentation = getFirstArg(finder, callNode);
					break;
				case VCALLNODE:
					VCallNode vcallNode = (VCallNode) n;
					// A call to 'ensurable' adds 'ensure' parameter
					if(ENSURABLE.equals(vcallNode.getName()))
						parameterMap.put("ensure", new PPTypeInfo.Entry("", false, false));
					break;
				case INSTASGNNODE:
					InstAsgnNode docNode = (InstAsgnNode) n;
					if("doc".equals(docNode.getName())) {
						typeDocumentation = getStringArgDefault(finder, docNode.getValue(), "");
					}
					break;
				default:
					break;
			}
		}
		if(typeDocumentation == null)
			typeDocumentation = "";
		PPTypeInfo typeInfo = new PPTypeInfo(typeName, typeDocumentation, propertyMap, parameterMap);
		return typeInfo;
	}

	/**
	 * Loads 'newmetaparam' from the ruby class 'Type'
	 *
	 * @param root
	 * @return
	 */
	public PPTypeInfo findMetaTypeInfo(Node root) {
		Map<String, PPTypeInfo.Entry> parameterMap = Maps.newHashMap();
		RubyModuleFinder moduleFinder = new RubyModuleFinder(root);
		Node module = moduleFinder.findModule(new String[] { "Puppet" });
		for(Node n : module.childNodes()) {
			if(n.getNodeType() == NodeType.NEWLINENODE)
				n = ((NewlineNode) n).getNextNode();
			if(n.getNodeType() == NodeType.CLASSNODE) {
				ClassNode classNode = (ClassNode) n;
				// could check if this is the class 'Type' but somewhat
				// meaningless
				// as this code is only called for the Type.rb file anyway.
				// classNode.getCPath();
				for(Node bn : classNode.getBody().childNodes()) {
					if(bn.getNodeType() == NodeType.NEWLINENODE)
						bn = ((NewlineNode) bn).getNextNode();
					if(bn.getNodeType() == NodeType.FCALLNODE) {
						FCallNode callNode = (FCallNode) bn;
						if("newmetaparam".equals(callNode.getName())) {
							parameterMap.put(getFirstArg(moduleFinder, callNode), getEntry(moduleFinder, callNode));

						}
					}

				}
			}
		}
		return new PPTypeInfo("Type", "", null, parameterMap);

	}

	public List<PPTypeInfo> findNagiosTypeInfo(Node root) {

		RubyCallFinder callFinder = new RubyCallFinder(root);
		// style 1

		List<PPTypeInfo> result = Lists.newArrayList();
		for(GenericCallNode newTypeCall : callFinder.findCalls(PUPPET_NAGIOS_NEWTYPE_FQN)) {
			if(newTypeCall.isValid()) {
				// should have at least one argument, the (raw) name of the
				// type
				String typeName = getFirstArg(callFinder, newTypeCall);
				if(typeName != null) { // just in case there is something
					// really wrong in parsing
					PPTypeInfo typeInfo = createNagiosTypeInfo(callFinder, safeGetBodyNode(newTypeCall), "nagios_" + typeName);
					if(typeInfo != null)
						result.add(typeInfo);
				}
			}

		}
		return result;
	}

	/**
	 * Finds provider info the form:<br/>
	 * <code>
	 * Puppet::Type.type(:typename).provide
	 * </code>
	 *
	 * @param root
	 * @return
	 */
	public List<PPProviderInfo> findProviderInfo(Node root) {
		RubyCallFinder callFinder = new RubyCallFinder(root);
		List<PPProviderInfo> result = null;
		for(GenericCallNode providerCall : callFinder.findCalls(PUPPET_PROVIDE_FQN)) {
			if(providerCall == null || !providerCall.isValid())
				continue;

			String providerName = getFirstArg(callFinder, providerCall);
			if(providerName == null)
				continue;

			Node node = providerCall.getNode();
			if(!(node instanceof CallNode))
				continue;

			Node receiver = ((CallNode) node).getReceiver();
			GenericCallNode typeCall = null;
			if(receiver instanceof CallNode)
				typeCall = new GenericCallNode((CallNode) receiver);
			else if(receiver instanceof FCallNode)
				typeCall = new GenericCallNode((FCallNode) receiver);

			if(typeCall == null || !typeCall.isValid())
				continue;

			String typeName = getFirstArg(callFinder, typeCall);
			if(typeName == null)
				continue;

			if(result == null)
				result = Lists.newArrayList();
			result.add(createProviderInfo(callFinder, safeGetBodyNode(providerCall), typeName, providerName));
		}
		return result == null
			? Collections.<PPProviderInfo> emptyList()
			: result;
	}

	/**
	 * Finds type info in one of the two forms:<br/>
	 * <code>module Puppet
	 *     newtype(:typename)
	 *     ...
	 * </code><br/>
	 * or<br/>
	 * <code>
	 * Puppet::Type.newtype(:typename)
	 * </code><br/>
	 * where the call may (but is not required to) take place in the Puppet
	 * module.
	 *
	 * @param root
	 * @return
	 */
	public PPTypeInfo findTypeInfo(Node root) {

		RubyCallFinder callFinder = new RubyCallFinder(root);
		// style 1
		GenericCallNode newTypeCall = callFinder.findCall(PUPPET_NEWTYPE_FQN);
		if(newTypeCall == null || !newTypeCall.isValid()) {
			// style 2
			newTypeCall = callFinder.findCall("Puppet", NEWTYPE);
			if(newTypeCall == null || !newTypeCall.isValid())
				return null;
		}

		// should have at least one argument, the name of the type
		String typeName = getFirstArg(callFinder, newTypeCall);
		if(typeName == null)
			return null;

		return createTypeInfo(callFinder, safeGetBodyNode(newTypeCall), typeName);
	}

	/**
	 * Finds a property addition to a type. Returns a partially filled
	 * PPTypeInfo (name, and a single property).
	 * TODO: Could be simplified and reuse the RubyCallFinder (if it is made
	 * capable of handling calls to Puppet::Type.type(:name) inside a module
	 * Puppet (currently, it will think this is a call to
	 * Puppet::Puppet::Type.type(:name).
	 *
	 * @param root
	 * @return PPTypeInfo partially filled, or null, if there were no property
	 *         addition found.
	 */
	public List<PPTypeInfo> findTypePropertyInfo(Node root) {
		List<PPTypeInfo> result = Lists.newArrayList();
		RubyModuleFinder moduleFinder = new RubyModuleFinder(root);
		Node module = moduleFinder.findModule(new String[] { "Puppet" });

		// Some property additions are in "Puppet" modules, some are not
		if(module == null)
			module = root.getNodeType() == NodeType.ROOTNODE
				? ((RootNode) root).getBody()
				: root;
		OpCallVisitor opCallVisitor = new OpCallVisitor(moduleFinder.getConstEvaluator());
		for(Node n1 : module.childNodes()) {
			if(n1.getNodeType() == NodeType.NEWLINENODE)
				n1 = ((NewlineNode) n1).getNextNode();
			Iterable<Node> nodeIterable = null;
			if(n1.getNodeType() == NodeType.BLOCKNODE)
				nodeIterable = ((BlockNode) n1).childNodes();
			else
				nodeIterable = Lists.newArrayList(n1);
			for(Node n : nodeIterable) {
				if(n.getNodeType() == NodeType.NEWLINENODE)
					n = ((NewlineNode) n).getNextNode();

				if(n.getNodeType() == NodeType.CALLNODE) {
					CallNode callNode = (CallNode) n;
					if(NEWPROPERTY.equals(callNode.getName())) {
						CallNode typeCall = opCallVisitor.findOpCall(callNode.getReceiver(), "type", "Puppet", "Type");
						if(typeCall == null)
							continue;
						String typeName = getFirstArg(moduleFinder, typeCall);
						if(typeName == null)
							continue;
						Map<String, PPTypeInfo.Entry> propertyMap = Maps.newHashMap();
						propertyMap.put(getFirstArg(moduleFinder, callNode), getEntry(moduleFinder, callNode));
						result.add(new PPTypeInfo(typeName, "", propertyMap, null));
						continue;
					}
					if(NEWPARAM.equals(callNode.getName())) {
						CallNode typeCall = opCallVisitor.findOpCall(callNode.getReceiver(), "type", "Puppet", "Type");
						if(typeCall == null)
							continue;
						String typeName = getFirstArg(moduleFinder, typeCall);
						if(typeName == null)
							continue;
						Map<String, PPTypeInfo.Entry> parameterMap = Maps.newHashMap();
						parameterMap.put(getFirstArg(moduleFinder, callNode), getEntry(moduleFinder, callNode));
						result.add(new PPTypeInfo(typeName, "", null, parameterMap));
						continue;
					}
					if(NEWCHECK.equals(callNode.getName())) {
						CallNode typeCall = opCallVisitor.findOpCall(callNode.getReceiver(), "type", "Puppet", "Type");
						if(typeCall == null)
							continue;
						String typeName = getFirstArg(moduleFinder, typeCall);
						if(typeName == null)
							continue;
						Map<String, PPTypeInfo.Entry> parameterMap = Maps.newHashMap();
						parameterMap.put(getFirstArg(moduleFinder, callNode), getEntry(moduleFinder, callNode));
						result.add(new PPTypeInfo(typeName, "", null, parameterMap));
					}
					// NOTE: this does probably never occur
					if(ENSURABLE.equals(callNode.getName())) {
						CallNode typeCall = opCallVisitor.findOpCall(callNode.getReceiver(), "type", "Puppet", "Type");
						if(typeCall == null)
							continue;
						String typeName = getFirstArg(moduleFinder, typeCall);
						if(typeName == null)
							continue;
						Map<String, PPTypeInfo.Entry> parameterMap = Maps.newHashMap();
						parameterMap.put("ensure", getEntry(moduleFinder, callNode));
						result.add(new PPTypeInfo(typeName, "", parameterMap, null));

					}
				}
			}
		}
		return result;

	}

	/**
	 * Returns the first String argument from a node with arguments, or null if
	 * there are no arguments or the argument list was not a list.
	 *
	 * @param callNode
	 * @return
	 */
	private List<String> getArgs(RubyFinder finder, IArgumentNode callNode) {
		Object result = finder.getConstEvaluator().eval(callNode.getArgs());
		if(!(result instanceof List<?>))
			return Collections.emptyList();
		@SuppressWarnings("unchecked")
		List<String> list = (List<String>) result;
		return list;
	}

	PPTypeInfo.Entry getEntry(RubyFinder finder, BlockAcceptingNode callNode) {
		String desc = "";
		boolean namevar = false;
		Node bodyNode = safeGetBodyNode(callNode);
		if(bodyNode != null)
			for(Node n : bodyNode.childNodes()) {
				if(n.getNodeType() == NodeType.NEWLINENODE)
					n = ((NewlineNode) n).getNextNode();
				if(n.getNodeType() == NodeType.FCALLNODE) {
					FCallNode cn = (FCallNode) n;
					if(DESC.equals(cn.getName()))
						desc = getFirstArgDefault(finder, cn, "");
					// return new PPTypeInfo.Entry(getFirstArgDefault(cn, ""),
					// false);
				}
				else if(n.getNodeType() == NodeType.VCALLNODE) {
					VCallNode vn = (VCallNode) n;
					if("isnamevar".equals(vn.getName()))
						namevar = true;
				}

			}

		return new PPTypeInfo.Entry(desc, false, namevar);
	}

	/**
	 * Returns the first String argument from a node with arguments, or null if
	 * there are no arguments or the argument list was not a list.
	 *
	 * @param callNode
	 * @return
	 */
	private String getFirstArg(RubyFinder finder, IArgumentNode callNode) {
		List<String> args = getArgs(finder, callNode);
		return args.isEmpty()
			? null
			: args.get(0);
	}

	private String getFirstArgDefault(RubyFinder finder, IArgumentNode callNode, String defaultValue) {
		String x = getFirstArg(finder, callNode);
		return x == null
			? defaultValue
			: x;
	}

	private String getStringArg(RubyFinder finder, Node n) {
		Object x = finder.getConstEvaluator().eval(n);
		if(!(x instanceof String))
			return null;
		return (String) x;
	}

	private String getStringArgDefault(RubyFinder finder, Node n, String defaultValue) {
		String x = getStringArg(finder, n);
		return x == null
			? defaultValue
			: x;
	}

	private Node safeGetBodyNode(BlockAcceptingNode node) {
		Node n = node.getIter();
		if(n == null)
			return null;
		switch(n.getNodeType()) {
			case ITERNODE:
				return ((IterNode) n).getBody();
			case BLOCKPASSNODE:
				return ((BlockPassNode) n).getBody();
			default:
				return null;
		}
	}

}
