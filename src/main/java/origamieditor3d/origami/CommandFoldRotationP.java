package origamieditor3d.origami;

public class CommandFoldRotationP extends CommandFold {
	int phi;
	int polygonIndex;
	
	static {
		foldId = 4;
	}
	
	public CommandFoldRotationP(double[] ppoint, double[] pnormal,  int polygonIndex, int phi, OrigamiGen1 origami) {

		this.ppoint = ppoint;
		this.pnormal = pnormal;
		this.phi = phi;
		this.polygonIndex = polygonIndex;
		this.origami = origami;

	}
	public void execute() {
		origami.internalRotationFold(ppoint, pnormal, phi, polygonIndex);
	}
	public void undo() {}
	

	@Override
	public int getPhi() {
		return this.phi;
	}
	
	@Override
	public int getPolygonIndex() {
		return this.polygonIndex;
	}

}

