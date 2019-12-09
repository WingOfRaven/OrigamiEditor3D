package origamieditor3d.origami;

public abstract class CommandFold implements Command, Cloneable {
	protected double [] ppoint;
	protected double [] pnormal;
	protected OrigamiGen1 origami;

	public CommandFold(OrigamiGen1 origami) { setOrigami(origami); }

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public double[] getPpoint() {
		return this.ppoint;
	}

	public double[] getPnormal() {
		return this.pnormal;
	}
	
	public int getPolygonIndex() {
		return 0;
	}
	
	public int getPhi() {
		return 0;
	}

	public void setOrigami(OrigamiGen1 origami) { this.origami = origami; }
}
