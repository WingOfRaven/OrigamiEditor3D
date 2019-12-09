package origamieditor3d.origami;

public class CommandFoldRotationP extends CommandFold {
	static public final int foldID = 4;

	int phi;
	int polygonIndex;

	public CommandFoldRotationP(double[] ppoint, double[] pnormal,  int polygonIndex, int phi, OrigamiGen1 origami) {
		super(origami);
		this.ppoint = ppoint;
		this.pnormal = pnormal;
		this.phi = phi;
		this.polygonIndex = polygonIndex;
	}

	@Override
	public int getFoldID() {
		return foldID;
	}

	@Override
	public void execute() {
		origami.internalRotationFold(ppoint, pnormal, phi, polygonIndex);
	}

	@Override
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

