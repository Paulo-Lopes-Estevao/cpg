/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *                    $$$$$$\  $$$$$$$\   $$$$$$\
 *                   $$  __$$\ $$  __$$\ $$  __$$\
 *                   $$ /  \__|$$ |  $$ |$$ /  \__|
 *                   $$ |      $$$$$$$  |$$ |$$$$\
 *                   $$ |      $$  ____/ $$ |\_$$ |
 *                   $$ |  $$\ $$ |      $$ |  $$ |
 *                   \$$$$$   |$$ |      \$$$$$   |
 *                    \______/ \__|       \______/
 *
 */

package de.fraunhofer.aisec.cpg.passes.scopes;

import de.fraunhofer.aisec.cpg.graph.ConstructorDeclaration;
import de.fraunhofer.aisec.cpg.graph.Declaration;
import de.fraunhofer.aisec.cpg.graph.FieldDeclaration;
import de.fraunhofer.aisec.cpg.graph.FunctionDeclaration;
import de.fraunhofer.aisec.cpg.graph.MethodDeclaration;
import de.fraunhofer.aisec.cpg.graph.NamespaceDeclaration;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.ParamVariableDeclaration;
import de.fraunhofer.aisec.cpg.graph.RecordDeclaration;
import de.fraunhofer.aisec.cpg.graph.ValueDeclaration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Is a scope where local variables can be declared and independent from specific language
 * constructs. Works for if, for, and extends to the block scope
 */
public class ValueDeclarationScope extends Scope {

  private static final Logger log = LoggerFactory.getLogger(ValueDeclarationScope.class);

  @NonNull private List<ValueDeclaration> valueDeclarations = new ArrayList<>();

  public ValueDeclarationScope(Node node) {
    this.astNode = node;
  }

  @NonNull
  public List<ValueDeclaration> getValueDeclarations() {
    return valueDeclarations;
  }

  public void setValueDeclarations(@NonNull List<ValueDeclaration> valueDeclarations) {
    this.valueDeclarations = valueDeclarations;
  }

  public void addDeclaration(@NonNull Declaration declaration) {
    if (declaration instanceof ValueDeclaration) {
      addValueDeclaration((ValueDeclaration) declaration);
    } else {
      log.error("A non ValueDeclaration can not be added to a DeclarationScope");
    }
  }

  /**
   * THe value declarations are only set in the ast node if the handler of the ast node may not know
   * the outer
   *
   * @param valueDeclaration
   */
  public void addValueDeclaration(ValueDeclaration valueDeclaration) {
    this.valueDeclarations.add(valueDeclaration);
    if (astNode instanceof NamespaceDeclaration) {
      NamespaceDeclaration namespaceD = (NamespaceDeclaration) astNode;

      if (valueDeclaration instanceof FieldDeclaration) {
        addByNameAndReplaceIfNotContained(namespaceD.getFields(), (FieldDeclaration) valueDeclaration);
      } else if (valueDeclaration instanceof FunctionDeclaration) {
        addByNameAndReplaceIfNotContained(namespaceD.getFunctions(), (FunctionDeclaration) valueDeclaration);
      }

    } else if (astNode instanceof RecordDeclaration) {
      RecordDeclaration recordD = (RecordDeclaration) astNode;
      if (valueDeclaration instanceof ConstructorDeclaration) {
        addByNameAndReplaceIfNotContained(recordD.getConstructors(), (ConstructorDeclaration) valueDeclaration);
      } else if (valueDeclaration instanceof MethodDeclaration) {
        addByNameAndReplaceIfNotContained(recordD.getMethods(), (MethodDeclaration) valueDeclaration);
      } else if (valueDeclaration instanceof FieldDeclaration) {
        addByNameAndReplaceIfNotContained(recordD.getFields(), (FieldDeclaration) valueDeclaration);
      }

    } else if (astNode instanceof FunctionDeclaration) {
      FunctionDeclaration functionD = (FunctionDeclaration) astNode;
      if (valueDeclaration instanceof ParamVariableDeclaration) {
        addByNameAndReplaceIfNotContained(functionD.getParameters(), (ParamVariableDeclaration) valueDeclaration);
      }
    } else if (this instanceof GlobalScope) {
      // Here ther is no ast-node
    }
    /*
     There are nodes where we do not set the declaration when storing them in the scope,
     mostly for structures that have a single value-declaration: WhileStatement, DoStatement,
     ForStatement, SwitchStatement; and others where the location of declaration is somewhere
     deeper in the AST-subtree: CompoundStatement, AssertStatement.
    */
  }

  protected <T extends Node> void addByNameAndReplaceIfNotContained(Collection<T> collection, T nodeToAdd) {
    if (!collection.contains(nodeToAdd)) {
      List<Node> existing = collection.stream().filter(node -> node.getName().equals(nodeToAdd)).collect(Collectors.toList());
      if(existing.size() == 0){
        collection.add(nodeToAdd);
      }else {
        existing.stream().forEach(toRemove -> collection.remove(toRemove));
        collection.add(nodeToAdd);
      }
    }
  }
}