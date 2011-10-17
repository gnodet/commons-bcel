/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 *
 */
/* Generated By:JJTree: Do not edit this line. ASTFunAppl.java */
/* JJT: 0.3pre1 */

package Mini;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.Type;

/**
 *
 * @version $Id$
 * @author <A HREF="mailto:m.dahm@gmx.de">M. Dahm</A>
 */
public class ASTFunAppl extends ASTExpr implements MiniParserTreeConstants,
  org.apache.bcel.Constants {
  private ASTIdent name;
  private Function function; // Points to Function in environment

  // Generated methods
  ASTFunAppl(int id) {
    super(id);
  }

  ASTFunAppl(MiniParser p, int id) {
    super(p, id);
  }

  public static Node jjtCreate(MiniParser p, int id) {
    return new ASTFunAppl(p, id);
  }

  ASTFunAppl(ASTIdent name, Function function, ASTExpr[] exprs) {
    this(JJTFUNAPPL);

    this.name     = name;
    this.function = function;
    this.exprs    = exprs;
  }

  @Override
  public String toString() {
    return jjtNodeName[id] + " " + name.getName();
  }

  /**
   * Overrides ASTExpr.closeNode()
   */
  @Override
  public void closeNode() {
    name = (ASTIdent)children[0];

    if(children.length > 1) {
      exprs = new ASTExpr[children.length - 1];
      System.arraycopy(children, 1, exprs, 0, children.length - 1);
    }

    children=null; // Throw away old reference
  }

  /**
   * Overrides ASTExpr.traverse()
   */
  @Override
  public ASTExpr traverse(Environment env) {
    String   fname = name.getName();
    EnvEntry entry = env.get(fname);

    this.env = env;

    if(entry == null) {
        MiniC.addError(name.getLine(), name.getColumn(),
                 "Applying unknown function " + fname + ".");
    } else {
      if(!(entry instanceof Function)) {
        MiniC.addError(name.getLine(), name.getColumn(),
                 "Applying non-function " + fname + ".");
    } else {
        int      len = (exprs != null)? exprs.length : 0;
        Function fun = (Function)entry;

        if(len != fun.getNoArgs()) {
        MiniC.addError(name.getLine(), name.getColumn(),
                   "Function " + fname + " expects " + fun.getNoArgs() +
                   " arguments, you supplied " + len + ".");
    } else { // Adjust references
          function = fun;
          name     = fun.getName();
        }
      }
    }

    if(exprs != null) {
        for(int i=0; i < exprs.length; i++) {
            exprs[i] = exprs[i].traverse(env);
        }
    }

    return this;
  }

  /**
   * Second pass
   * Overrides AstExpr.eval()
   * @return type of expression
   * @param expected type
   */
  @Override
  public int eval(int expected) {
    String     fname = name.getName();
    Function   f     = function;
    ASTIdent   fun   = f.getName();
    ASTIdent[] args  = f.getArgs();
    int        t     = fun.getType();

    is_simple = true; // Only true if all arguments are simple expressions

    // Check arguments
    if(exprs != null) {
      for(int i=0; i < exprs.length; i++) { // length match checked in previous pass
        int expect = args[i].getType();     // May be T_UNKNOWN
        int t_e    = exprs[i].eval(expect); // May be T_UNKNOWN

        if((expect != T_UNKNOWN) && (t_e != expect)) {
        MiniC.addError(exprs[i].getLine(), exprs[i].getColumn(),
                 "Argument " + (i + 1) + " in application of " + fname +
                 " is not of type " + TYPE_NAMES[expect] + " but " +
                 TYPE_NAMES[t_e]);
    } else {
        args[i].setType(t_e); // Update, may be identical
    }

        is_simple = is_simple && exprs[i].isSimple(); // Check condition
      }
    }

    if(t == T_UNKNOWN) {
        fun.setType(t = expected); // May be still T_UNKNOWN
    }

    return type = t;
  }

  /**
   * Fourth pass, produce Java code.
   */
  @Override
  public void code(StringBuffer buf) {
    String     fname = name.getName();
//    Function   f     = function;
//    ASTIdent[] args  = f.getArgs();

    if(fname.equals("READ")) {
        ASTFunDecl.push(buf, "_readInt()");
    } else if(fname.equals("WRITE")) {
      exprs[0].code(buf);
      ASTFunDecl.push(buf, "_writeInt(" + ASTFunDecl.pop() + ")");
    }
    else { // Normal function
      if(exprs != null) { // Output in reverse odrder
        for(int i = exprs.length - 1; i >= 0; i--) {
        exprs[i].code(buf);
    }
      }

      StringBuffer call = new StringBuffer(fname + "(");
      // Function call

      if(exprs != null) {
        for(int i=0; i < exprs.length; i++) {
          call.append(ASTFunDecl.pop());
          if(i < exprs.length - 1) {
        call.append(", ");
    }
        }
      }
      call.append(")");

      ASTFunDecl.push(buf, call.toString());
    }
  }

  /**
   * Fifth pass, produce Java byte code.
   */
  @Override
  public void byte_code(InstructionList il, MethodGen method, ConstantPoolGen cp) {
    String     fname = name.getName();
//    Function   f     = function;
    //ASTIdent   fun   = f.getName();
//    ASTIdent[] args  = f.getArgs();
    String     class_name = method.getClassName();

    if(fname.equals("READ")) {
        il.append(new INVOKESTATIC(cp.addMethodref(class_name,
                                                 "_readInt",
                                                 "()I")));
    } else if(fname.equals("WRITE")) {
      exprs[0].byte_code(il, method, cp);
      ASTFunDecl.pop();
      il.append(new INVOKESTATIC(cp.addMethodref(class_name,
                                                 "_writeInt",
                                                 "(I)I")));
    }
    else { // Normal function
      int size    = exprs.length;
      Type[] argv = null;

      if(exprs != null) {
        argv = new Type[size];

        for(int i=0; i < size; i++) {
          argv[i] = Type.INT;
          exprs[i].byte_code(il, method, cp);
        }

        //ASTFunDecl.push(size);
      }

      ASTFunDecl.pop(size);

      // Function call
      il.append(new INVOKESTATIC(cp.addMethodref(class_name,
                                                 fname,
                                                 Type.getMethodSignature(Type.INT,
                                                                         argv))));
    }

    ASTFunDecl.push();
  }

  // dump() inherited
  public ASTIdent getName()     { return name; }
  public Function getFunction() { return function; }
}