package origamieditor3d.origami;

public class CommandFoldMutilationP extends CommandFold {
	static {
		foldID = 7;
	}

	int polygonIndex;

	public CommandFoldMutilationP(double[] ppoint, double[] pnormal, int polygonIndex, OrigamiGen1 origami) {

		this.ppoint = ppoint;
		this.pnormal = pnormal;
		this.polygonIndex = polygonIndex;
		this.origami = origami;

	}

	@Override
	public void execute() {
		origami.internalMutilation(ppoint, pnormal, polygonIndex);
	}

	@Override
	public void undo() {}
	
	@Override
	public int getPolygonIndex() {
		return this.polygonIndex;
	}
}
