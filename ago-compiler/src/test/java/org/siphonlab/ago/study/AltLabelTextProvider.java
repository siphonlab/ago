/*
 * Copyright © 2026 Inshua (inshua@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.siphonlab.ago.study;

import org.antlr.v4.gui.TreeTextProvider;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.tree.Tree;
import org.antlr.v4.runtime.tree.Trees;
import org.antlr.v4.tool.Rule;
import org.antlr.v4.tool.ast.AltAST;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AltLabelTextProvider implements TreeTextProvider {
	protected final Parser parser;

	public AltLabelTextProvider(Parser parser) {
		this.parser = parser;
		//this.g = g;
	}

	public String[] getAltLabels(Rule r) {
		String[] altLabels = null;
		Map<String, List<Pair<Integer, AltAST>>> altLabelsMap = r.getAltLabels();
		if ( altLabelsMap!=null ) {
			altLabels = new String[r.getOriginalNumberOfAlts() + 1];
			for (String altLabel : altLabelsMap.keySet()) {
				List<Pair<Integer, AltAST>> pairs = altLabelsMap.get(altLabel);
				for (Pair<Integer, AltAST> pair : pairs) {
					altLabels[pair.a] = altLabel;
				}
			}
		}
		return altLabels;
	}

	@Override
	public String getText(Tree node) {
		String nodeText = Trees.getNodeText(node, Arrays.asList(parser.getRuleNames()));
		if ( node instanceof ParserRuleContext r) {
			String simpleName = r.getClass().getSimpleName();
			simpleName = simpleName.substring(0,simpleName.lastIndexOf("Context"));
			if(simpleName.equalsIgnoreCase(nodeText)){
				return nodeText;
			} else {
				return nodeText + ":" + simpleName;
			}
		}
		return nodeText;
	}

	private String getLabelForToken(Token token) {
		String text = token.getText();
		if (text.equals("<EOF>")) {
			return text;
		}

		String symbolicName = parser.getVocabulary().getSymbolicName(token.getType());
		if ( symbolicName==null ) { // it's a literal like ';' or 'return'
			return text;
		}
		if ( text.toUpperCase().equals(symbolicName) ) { // IMPORT:import
			return symbolicName;
		}
		return symbolicName + ":" + text;
	}
}