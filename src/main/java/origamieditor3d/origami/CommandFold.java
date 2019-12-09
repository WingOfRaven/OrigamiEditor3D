package origamieditor3d.origami;

public abstract class CommandFold implements Command {
	public abstract int getFoldID();

	protected double [] ppoint;
	protected double [] pnormal;
	protected OrigamiGen1 origami;

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
}
