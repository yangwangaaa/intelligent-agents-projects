/* Generated By:JJTree: Do not edit this line. ASTFalse.java */

package uchicago.src.sim.parameter.rpl;

public class ASTFalse extends SimpleNode {
  public ASTFalse(int id) {
    super(id);
  }

  public ASTFalse(RPLParser p, int id) {
    super(p, id);
  }

  public RPLObject getValue() {
    return new RPLBooleanValue(false);
  }
}
