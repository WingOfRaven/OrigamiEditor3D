package origamieditor3d.origami;

public abstract class CommandFold implements Command {
	
	double [] ppoint;
	double [] pnormal;
	public static int foldId = 0;
	OrigamiGen1 origami;
	
	@Override
	public double[] getPpoint() {
		return this.ppoint;
	}
	
	@Override
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
