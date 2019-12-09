package origamieditor3d.origami;

public class CommandFoldRotation extends CommandFold {
	
	int phi;
	
	static {
		foldId = 2;
	}
	public CommandFoldRotation(double[] ppoint, double[] pnormal, int phi, OrigamiGen1 origami) {

		this.ppoint = ppoint;
		this.pnormal = pnormal;
		this.phi = phi;
		this.origami = origami;

	}
	
	public void execute() {
		origami.internalRotationFold(ppoint, pnormal, phi);
	}
	public void undo() {}
	
	@Override
	public int getPhi() {
		return this.phi;
	}

}

