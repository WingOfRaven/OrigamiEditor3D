package origamieditor3d.origami;

public class CommandFoldRotationP extends CommandFold {
	int phi;
	int polygonIndex;
	public CommandFoldRotationP(double[] ppoint, double[] pnormal, int phi, int polygonIndex, OrigamiGen1 origami) {

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

}

