package origamieditor3d.origami;

public class CommandFoldReflectionP extends CommandFold {
	static public final int foldID = 3;

	int polygonIndex;

	public CommandFoldReflectionP(double[] ppoint, double[] pnormal, int polygonIndex, OrigamiGen1 origami) {
		super(origami);
		this.ppoint = ppoint;
		this.pnormal = pnormal;
		this.polygonIndex = polygonIndex;
	}

	@Override
	public int getFoldID() {
		return foldID;
	}

	@Override
	public void execute() {
		origami.internalReflectionFold(ppoint, pnormal, polygonIndex);
	}

	@Override
	public void undo() {}
	
	@Override
	public int getPolygonIndex() {
		return this.polygonIndex;
	}

}

